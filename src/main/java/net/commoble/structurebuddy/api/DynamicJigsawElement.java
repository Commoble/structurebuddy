package net.commoble.structurebuddy.api;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.util.CodecBuddy;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Elements of {@link DynamicJigsawPool} files.
 * Responsible for producing DynamicJigsawResults describing pieces which may be added to the jigsaw tree.
 */
public interface DynamicJigsawElement
{
	/**
	 * Type-dispatched Codec. Subcodecs can be registered to {@link StructureBuddyRegistries#DYNAMIC_JIGSAW_ELEMENT_TYPE}
	<pre>
	{
		"type": "yourmod:some_element_type",
		// additional fields as needed
	}
	</pre>
	 */
	public static final Codec<DynamicJigsawElement> CODEC = CodecBuddy.dispatch(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, DynamicJigsawElement::codec);
	
	/**
	 * {@return MapCodec for this type of DynamicJigsawElement, which has been registered to {@link StructureBuddyRegistries#DYNAMIC_JIGSAW_ELEMENT_TYPE}} 
	 */
	public abstract MapCodec<? extends DynamicJigsawElement> codec();
	
	/**
	 * This method is responsible for defining the contents and connections of a potential structure piece to be added to the jigsaw tree.
	 * If any randomness is to be applied that needs to be consistent across multiple chunks generating or a server reboot,
	 * this is where those random calls should be done.
	 * @param context DynamicJigsawBakeContext
	 * @return DynamicJigsawBakeResult containing the size, contents, and connections of the structure piece to be added.
	 */
	public abstract DynamicJigsawResult bake(DynamicJigsawBakeContext context);

	/**
	 * Context given to {@link DynamicJigsawElement#bake}
	 * @param generationContext GenerationContext
	 * @param remainingSpace OctreeView of the remaining space available where this child piece can generate. Absolute world coordinates.
	 * If this is child is generating within the interior of another piece, this will be restricted to the bounds of that parent piece.
	 * Please do not attempt to mutate this as it will be reused across multiple generation attempts and your piece may not be selected.
	 * For the start piece, a space of infinite size is provided as the actual space is calculated after the first piece forms,
	 * so be mindful that the given space may be larger than actual space available.
	 * For pieces of fixed size, it is generally not necessary to use this to verify that your piece will fit into the available space
	 * as this is done by the jigsaw assembler anyway;
	 * the purpose of this is more for shaping dynamic pieces into the available space
	 * @param parent Jigsaw information of the parent jigsaw (including pos and orientation). Absolute world coordinates.
	 * Will be null if this is the start piece.
	 * (the connection of the parent points to this jigsaw piece, which is the child)
	 * @param remainingConnections Jigsaw connections which are yet to be processed.
	 * Interior connections are ignored,
	 * and list will be empty if this piece is an interior piece.
	 * Can be used to attempt to loop back onto an existing piece.
	 * @param rotation Rotation
	 * @param liquidSettings LiquidSettings
	 */
	public static record DynamicJigsawBakeContext(
		GenerationContext generationContext,
		AvailableSpace remainingSpace,
		@Nullable JigsawConnectionToChild parent,
		Supplier<List<JigsawConnectionToChild>> remainingConnections,
		Rotation rotation,
		LiquidSettings liquidSettings) {}
}
