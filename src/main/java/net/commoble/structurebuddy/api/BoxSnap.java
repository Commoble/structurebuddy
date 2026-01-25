package net.commoble.structurebuddy.api;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.content.StructureTemplateBoxElement;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.FrontAndTop;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Indicates which directions {@link StructureTemplateBoxElement} will snap to, if any,
 * when the structure piece is smaller than the available BoundingBox and a bounding surface is available. 
 */
public sealed interface BoxSnap
{
	/**
	 * Valid formats:
	 * <pre>
	 * {
	 *   "snap": "none", // default, no snapping is done
	 *   "snap": "wall", // snaps to random available wall
	 *   "snap": "floor", // snaps to floor if available
	 *   "snap": "ceiling", // snaps to ceiling if available
	 *   "snap": {"front_jigsaw": "jigsaw_name", "top_jigsaw": "jigsaw_name"} // snaps in direction of front and/or top orientations of named jigsaw blocks, both fields optional 
	 * }
	 * </pre>
	 */
	public static final Codec<BoxSnap> CODEC = Codec.either(
			Codec.either(
				JigsawBoxSnap.CODEC,
				FaceBoxSnap.CODEC),
			NoneBoxSnap.CODEC)
		.xmap(
			eitherEither -> eitherEither.map(
				jigsawOrFace -> jigsawOrFace.map(Function.identity(), Function.identity()),
				none -> none),
			snap -> switch(snap) {
				case NoneBoxSnap none -> Either.right(none);
				case FaceBoxSnap face -> Either.left(Either.right(face));
				case JigsawBoxSnap jigsaw -> Either.left(Either.left(jigsaw));
			});
	
	/**
	 * {@return SnapResult if possible, or null if box cannot be placed with the given context (only JigsawBoxSnap returns null)}
	 * @param jigsaws List of jigsaw blocks in the structure template
	 * @param boundingSurfaces Directions of solid surfaces adjacent to the generation box
	 * @param random RandomSource
	 */
	public abstract @Nullable SnapResult getSnap(List<JigsawConnectionToParent> jigsaws, EnumSet<Direction> boundingSurfaces, RandomSource random);
	
	/**
	 * BoxSnap indicating no snapping is to be done
	 */
	public static enum NoneBoxSnap implements BoxSnap
	{
		/** singleton instance **/
		NONE;
		
		/**
		 * <pre>
		 * {
		 * 	"snap": "none"
		 * }
		 * </pre>
		 */
		public static final Codec<NoneBoxSnap> CODEC = Codec.STRING.comapFlatMap(
			s -> s.equals("none") ? DataResult.success(NONE) : DataResult.error(() -> "Invalid boxsnap name " + s),
			snap -> "none");

		@Override
		public @NonNull SnapResult getSnap(List<JigsawConnectionToParent> jigsaws, EnumSet<Direction> boundingSurfaces, RandomSource random)
		{
			return SnapResult.NONE;
		}
	}
	
	/**
	 * BoxSnap indicating snapping can be done to a wall, floor, or ceiling
	 */
	public static enum FaceBoxSnap implements BoxSnap
	{
		/** Snap to floor if available **/
		FLOOR(AttachFace.FLOOR),
		/** Snap to random available wall **/
		WALL(AttachFace.WALL),
		/** Snap to ceiling if available **/
		CEILING(AttachFace.CEILING);
		
		private final AttachFace face;
		
		private FaceBoxSnap(AttachFace face)
		{
			this.face = face;
		}
		
		/** {@return AttachFace} **/
		public AttachFace face()
		{
			return this.face;
		}
		
		/**
		 * <pre>
		 * {
		 * 	"snap": "wall"
		 * }
		 * </pre>
		 */
		public static final Codec<FaceBoxSnap> CODEC = StringRepresentable.fromValues(AttachFace::values)
			.xmap(FaceBoxSnap::fromAttachFace, FaceBoxSnap::face);
		
		/**
		 * {@return FaceBoxSnap for the given AttachFace}
		 * @param face AttachFace
		 */
		public static FaceBoxSnap fromAttachFace(AttachFace face)
		{
			return switch(face) {
				case WALL -> FaceBoxSnap.WALL;
				case FLOOR -> FaceBoxSnap.FLOOR;
				case CEILING -> FaceBoxSnap.CEILING;
			};
		}

		@Override
		public @NonNull SnapResult getSnap(List<JigsawConnectionToParent> jigsaws, EnumSet<Direction> boundingSurfaces, RandomSource random)
		{
			SnapResult result = SnapResult.NONE;
			Collection<Direction> snapDirs = switch(this.face) {
				case FLOOR -> List.of(Direction.DOWN);
				case CEILING -> List.of(Direction.UP);
				case WALL -> Direction.allShuffled(random).stream().filter(dir -> dir.getAxis() != Axis.Y).toList();
			};
			for (Direction dir : snapDirs)
			{
				if (boundingSurfaces.contains(dir))
				{
					result = result.withDirectionIfNotChosen(dir);
				}
			}
			return result;
		}
	}
	
	/**
	 * BoxSnap which tries to align jigsaw blocks in the rotated structure template to available surfaces.
	 * At least one of the two optional jigsaw names must be specified.
	 * If multiple jigsaw blocks exist with a given name, they will be iterated over in random order
	 * until snaps on all three axes are found (or the jigsaws are exhausted).
	 * You could conceivably include multiple jigsaws facing different directions to randomize snap directions in this way.
	 * Relative positions of jigsaws are not checked nor are they required to be externally-facing jigsaws,
	 * only the names and facings are checked. 
	 * If no snapping can be done by the JigsawBoxSnap, then a *null* SnapResult is returned,
	 * indicating the box element cannot be placed with the given context.
	 * "Front" and "Top" terms are used to be consistent with {@link FrontAndTop};
	 * be aware that for vertical jigsaw blocks, front is up/down and top is in front.
	 * @param frontJigsaw Optional name of jigsaw block(s) to use the front direction of for snapping
	 * @param topJigsaw Optional name of jigsaw block(s) to use the top direction of for snapping
	 * @param strictness How strict snapping requirements are, see Strictness for specifics.
	 */
	public static record JigsawBoxSnap(Optional<Identifier> frontJigsaw, Optional<Identifier> topJigsaw, Strictness strictness) implements BoxSnap
	{
		/** Indicator of how strict placability requirements for JigsawBoxSnaps are **/
		public static enum Strictness implements StringRepresentable
		{
			/** Indicates a box can be placed even if no surfaces found for any jigsaws **/
			NONE,
			/** Indicates a box can be placed if at least one jigsaw for a given name is found which matches a surface **/
			ANY,
			/** Indicates a box can only be placed if all jigsaws matching a specified name can be matched to a surface **/ 
			ALL;
			
			/** Serializes as string "none", "any", or "all" **/
			public static final Codec<Strictness> CODEC = StringRepresentable.fromValues(Strictness::values);

			@Override
			public String getSerializedName()
			{
				return this.name().toLowerCase(Locale.ROOT);
			}
		}
		
		/**
		 * <pre>
		 * {
		 * 	"snap": {
		 * 		"front_jigsaw": "snap_front", // required
		 * 		"top_jigsaw": "snap_top", // required
		 * 		"strictness": "all" // can be "none", "any", or "all", defaults to "all" if not specified
		 * 	}
		 * }
		 * </pre>
		 */
		// explicit generics are here so both eclipse and javac can compile this
		public static final Codec<JigsawBoxSnap> CODEC = RecordCodecBuilder.<JigsawBoxSnap>create(builder -> builder.<Optional<Identifier>,Optional<Identifier>,Strictness>group(
				Identifier.CODEC.optionalFieldOf("front_jigsaw").forGetter(JigsawBoxSnap::frontJigsaw),
				Identifier.CODEC.optionalFieldOf("top_jigsaw").forGetter(JigsawBoxSnap::frontJigsaw),
				Strictness.CODEC.optionalFieldOf("strictness", Strictness.ALL).forGetter(JigsawBoxSnap::strictness)
			).apply(builder, JigsawBoxSnap::new))
			.validate(snap -> snap.frontJigsaw.isEmpty() && snap.topJigsaw.isEmpty()
					? DataResult.error(() -> "JigsawBoxSnap must specify either front_jigsaw or top_jigsaw")
					: DataResult.success(snap));
		
		/**
		 * {@return JigsawBoxSnap snapping in direction of named jigsaw's top}
		 * @param jigsawName Identifier of jigsaw block to snap to the front facing of
		 */
		public static JigsawBoxSnap front(Identifier jigsawName)
		{
			return new JigsawBoxSnap(Optional.of(jigsawName), Optional.empty(), Strictness.ALL);
		}
		
		/**
		 * {@return JigsawBoxSnap snapping in direction of named jigsaw's top}
		 * @param jigsawName Identifier of jigsaw block to snap to the top facing of
		 */
		public static JigsawBoxSnap top(Identifier jigsawName)
		{
			return new JigsawBoxSnap(Optional.empty(), Optional.of(jigsawName), Strictness.ALL);
		}
		
		/**
		 * {@return JigsawBoxSnap snapping in direction of named jigsaw's front and top}
		 * @param jigsawName Identifier of jigsaw block to snap to the front and top facing of
		 */
		public static JigsawBoxSnap frontAndTop(Identifier jigsawName)
		{
			return new JigsawBoxSnap(Optional.of(jigsawName), Optional.of(jigsawName), Strictness.ALL);
		}
		
		/**
		 * {@return JigsawBoxSnap snapping in direction of one jigsaw's front and another jigsaw's top}
		 * @param frontJigsaw Identifier of jigsaw block to snap to the front facing of
		 * @param topJigsaw Identifier of jigsaw block to snap to the top facing of
		 */
		public static JigsawBoxSnap frontAndTop(Identifier frontJigsaw, Identifier topJigsaw)
		{
			return new JigsawBoxSnap(Optional.of(frontJigsaw), Optional.of(topJigsaw), Strictness.ALL);
		}
		
		/**
		 * {@return JigsawBoxSnap with this instance's jigsaw names but with the given strictness}
		 * @param strictness Strictness the new JigsawBoxSnap will have
		 */
		public JigsawBoxSnap withStrictness(Strictness strictness)
		{
			return new JigsawBoxSnap(this.frontJigsaw, this.topJigsaw, strictness);
		}

		@Override
		public @Nullable SnapResult getSnap(List<JigsawConnectionToParent> jigsaws, EnumSet<Direction> boundingSurfaces, RandomSource random)
		{
			SnapResult result = SnapResult.NONE;
			@Nullable Identifier frontJigsawName = this.frontJigsaw.orElse(null);
			@Nullable Identifier topJigsawName = this.topJigsaw.orElse(null);
			for (JigsawConnectionToParent jigsaw : jigsaws)
			{
				Identifier name = jigsaw.name();
				if (frontJigsawName != null && frontJigsawName.equals(name))
				{
					Direction front = jigsaw.orientation().front();
					if (boundingSurfaces.contains(front))
					{
						result = result.withDirectionIfNotChosen(front);
					}
					else if (this.strictness == Strictness.ALL)
					{
						return null;
					}
				}
				if (topJigsawName != null && topJigsawName.equals(topJigsawName))
				{

					Direction top = jigsaw.orientation().top();
					if (boundingSurfaces.contains(top))
					{
						result = result.withDirectionIfNotChosen(top);
					}
					else if (this.strictness == Strictness.ALL)
					{
						return null;
					}
				}
			}
			return (result == SnapResult.NONE && this.strictness != Strictness.NONE)
				? null
				: result;
		}
	}
}