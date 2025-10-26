package net.commoble.structurebuddy.api;

import java.util.ArrayList;
import java.util.List;

import net.commoble.structurebuddy.api.content.EmptyPieceFiller;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.JigsawBlockInfo;

/**
 * Result of baking a dynamic jigsaw element.
 * If any connections to parent are compatible with the parent jigsaw being processed,
 * the baking jigsaw piece will be placed into the structure and further processing will be attempted
 * on this piece's children, if any
 * @param pieceFiller PieceFiller which will be serialized in the StructurePiece in region files,
 * and used later to fill the piece when overlapping chunks generate.
 * @param localBoundingBox BoundingBox of the structure piece.
 * Does not need to be in absolute world coordinates or any reference frame in particular,
 * will be moved to the correct location based on which jigsaw connector is chosen.
 * @param shuffledLocalConnectionsToParent List of possible jigsaw connections pointing to parent piece.
 * Does not need to be in absolute world coordinates,
 * but must be in the same reference frame relative to localBoundingBox.
 * Must also be contained within localBoundingBox,
 * otherwise otherwise-valid connections may be unexpectedly rejected.
 * due to optimization assumptions.
 * @param shuffledLocalConnectionsToChildren List of possible jigsaw connections pointing to child pieces.
 * Does not need to be in absolute world coordinates,
 * but must be in the same reference frame relative to localBoundingBox.
 * Must also be contained within localBoundingBox,
 * otherwise otherwise-valid connections may be unexpectedly rejected.
 * A connection to child can be in the same position as a connection to parent,
 * if connection to parent becomes used then connection to child at that pos will just be ignored.
 */
public record DynamicJigsawResult(PieceFiller pieceFiller, BoundingBox localBoundingBox, List<JigsawConnectionToParent> shuffledLocalConnectionsToParent, List<JigsawConnectionToChild> shuffledLocalConnectionsToChildren)
{
	/** Empty DynamicJigsawResult indicating no piece can be created or connected**/
	public static final DynamicJigsawResult EMPTY = new DynamicJigsawResult(EmptyPieceFiller.INSTANCE, BoundingBox.infinite(), List.of(), List.of());

	/** minecraft:empty as a child jigsaw name indicates it should not be a child, and as a target jigsaw name of a parent indicates it should not be a parent */
	public static final ResourceLocation EMPTY_NAME = ResourceLocation.withDefaultNamespace("empty");
	
	/**
	 * {@return DynamicJigsawResult with parent but no children}
	 * @param pieceFiller PieceFiller which will be serialized in the StructurePiece in region files,
	 * and used later to fill the piece when overlapping chunks generate.
	 * @param localBoundingBox BoundingBox of the structure piece.
	 * Does not need to be in absolute world coordinates or any reference frame in particular,
	 * will be moved to the correct location based on which jigsaw connector is chosen.
	 * @param shuffledLocalConnectionsToParent List of possible jigsaw connections pointing to parent piece.
	 * Does not need to be in absolute world coordinates,
	 * but must be in the same reference frame relative to localBoundingBox.
	 * Must also be contained within localBoundingBox,
	 * otherwise otherwise-valid connections may be unexpectedly rejected.
	 * due to optimization assumptions.
	 */
	public static DynamicJigsawResult withParents(PieceFiller pieceFiller, BoundingBox localBoundingBox, List<JigsawConnectionToParent> shuffledLocalConnectionsToParent)
	{
		return new DynamicJigsawResult(pieceFiller, localBoundingBox, shuffledLocalConnectionsToParent, List.of());
	}
	
	/**
	 * {@return DynamicJigsawResult with children but no parent (only valid for start pieces)}
	 * @param pieceFiller PieceFiller which will be serialized in the StructurePiece in region files,
	 * and used later to fill the piece when overlapping chunks generate.
	 * @param localBoundingBox BoundingBox of the structure piece.
	 * Does not need to be in absolute world coordinates or any reference frame in particular,
	 * will be moved to the correct location based on which jigsaw connector is chosen.
	 * @param shuffledLocalConnectionsToChildren List of possible jigsaw connections pointing to child pieces.
	 * Does not need to be in absolute world coordinates,
	 * but must be in the same reference frame relative to localBoundingBox.
	 * Must also be contained within localBoundingBox,
	 * otherwise otherwise-valid connections may be unexpectedly rejected.
	 * A connection to child can be in the same position as a connection to parent,
	 * if connection to parent becomes used then connection to child at that pos will just be ignored.
	 */
	public static DynamicJigsawResult withChildren(PieceFiller pieceFiller, BoundingBox localBoundingBox, List<JigsawConnectionToChild> shuffledLocalConnectionsToChildren)
	{
		return new DynamicJigsawResult(pieceFiller, localBoundingBox, List.of(), shuffledLocalConnectionsToChildren);
	}
	/**
	 * Just runs the normal constructor, but the name helps remember which order the connection list params are
	 * @param pieceFiller PieceFiller which will be serialized in the StructurePiece in region files,
	 * and used later to fill the piece when overlapping chunks generate.
	 * @param localBoundingBox BoundingBox of the structure piece.
	 * Does not need to be in absolute world coordinates or any reference frame in particular,
	 * will be moved to the correct location based on which jigsaw connector is chosen.
	 * @param shuffledLocalConnectionsToParent List of possible jigsaw connections pointing to parent piece.
	 * Does not need to be in absolute world coordinates,
	 * but must be in the same reference frame relative to localBoundingBox.
	 * Must also be contained within localBoundingBox,
	 * otherwise otherwise-valid connections may be unexpectedly rejected.
	 * due to optimization assumptions.
	 * @param shuffledLocalConnectionsToChildren List of possible jigsaw connections pointing to child pieces.
	 * Does not need to be in absolute world coordinates,
	 * but must be in the same reference frame relative to localBoundingBox.
	 * Must also be contained within localBoundingBox,
	 * otherwise otherwise-valid connections may be unexpectedly rejected.
	 * A connection to child can be in the same position as a connection to parent,
	 * if connection to parent becomes used then connection to child at that pos will just be ignored.
	 * @return DynamicJigsawResult
	 */
	public static DynamicJigsawResult withParentsAndChildren(PieceFiller pieceFiller, BoundingBox localBoundingBox, List<JigsawConnectionToParent> shuffledLocalConnectionsToParent, List<JigsawConnectionToChild> shuffledLocalConnectionsToChildren)
	{
		return new DynamicJigsawResult(pieceFiller, localBoundingBox, shuffledLocalConnectionsToParent, shuffledLocalConnectionsToChildren);
	}
	
	/**
	 * {@return this piece's local BoundingBox moved by the given offset}
	 * @param offset Vec3i offset to apply to the local bounding box
	 */
	public BoundingBox boundingBox(Vec3i offset)
	{
		return this.localBoundingBox.moved(offset.getX(), offset.getY(), offset.getZ());
	}
	
	/**
	 * {@return this piece's JigsawConnectionToChilds, moved by the given offset}
	 * @param offset Vec3i offset to apply to the child connection positions
	 */
	public List<JigsawConnectionToChild> offsetShuffledConnectionsToChildren(Vec3i offset)
	{
		List<JigsawConnectionToChild> results = new ArrayList<>();
		for (var localJigsaw : this.shuffledLocalConnectionsToChildren())
		{
			results.add(localJigsaw.moved(offset));
		}
		return results;
	}
	
	/**
	 * Adds a potential parent and child connection at the same position
	 * @param connectionsToParent List of connections to parent to add to
	 * @param connectionsToChildren List of connections to children to add to
	 * @param pos BlockPos of the jigsaw block. May be relative or absolute depending on context.
	 * @param orientation FrontAndTop state of the jigsaw.
	 * The front is the primary direction of the jigsaw, which must be the opposite of the parent jigsaw for them to match
	 * The top only matters if front is vertical and parent has JointType RIGID, in which case the tops must be the same to connect
	 * @param jointType JointType of the jigsaw. If RIGID and orientation front is vertical, orientation top must be the same for two jigsaws to match.
	 * @param name ResourceLocation of this jigsaw when used as a child connector, to be targeted by parent jigsaws
	 * @param targetPool ResourceKey of the DynamicJigsawPool this parent jigsaw will generate child jigsaws from.
	 * @param targetName ResourceLocation which a child jigsaw must have in order to be targeted by this parent jigsaw 
	 * @param placementPriority int designating when this jigsaw's child piece should be processed, after being selected, relative to other pieces. Higher = sooner.
	 * The child piece has already been selected and oriented within the jigsaw tree once this is used, so this causes the children of the child to be checked for fitting earlier, not the child itself.   
	 */
	public static void addParentOrChild(
		List<JigsawConnectionToParent> connectionsToParent,
		List<JigsawConnectionToChild> connectionsToChildren,
		BlockPos pos,
		FrontAndTop orientation,
		JointType jointType,
		ResourceLocation name,
		ResourceKey<DynamicJigsawPool> targetPool,
		ResourceLocation targetName,
		int placementPriority)
	{
		connectionsToParent.add(new JigsawConnectionToParent(pos, orientation, name, placementPriority));
		connectionsToChildren.add(new JigsawConnectionToChild(pos, orientation, jointType, targetPool, targetName));
	}
	
	/**
	 * Converts a JigsawBlockInfo from a StructureTemplate to dynamic jigsaw parent/child connections and adds them to lists.
	 * @param jigsaw JigsgawBlockInfo from a StructureTemplate
	 * @param selectableJigsawConnectionsToParents List of connections to parents
	 * @param connectionsToChildren List of connections to children
	 */
	public static void addConnectionsFromTemplateJigsaw(JigsawBlockInfo jigsaw, List<SelectableJigsawConnectionToParent> selectableJigsawConnectionsToParents, List<JigsawConnectionToChild> connectionsToChildren)
	{
		ResourceLocation name = jigsaw.name();
		ResourceLocation targetName = jigsaw.target();
		ResourceKey<StructureTemplatePool> targetPoolKey = jigsaw.pool();
		ResourceLocation targetPoolLocation = targetPoolKey.location();
		BlockPos pos = jigsaw.info().pos();
		FrontAndTop orientation = jigsaw.info().state().getValue(JigsawBlock.ORIENTATION);
		// pool could refer to minecraft:empty or structurebuddy:empty, just check the path
		if (!targetName.equals(EMPTY_NAME) && !targetPoolLocation.getPath().equals("empty"))
		{
			connectionsToChildren.add(new JigsawConnectionToChild(
				pos,
				orientation,
				jigsaw.jointType(),
				ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, targetPoolLocation),
				targetName
			));
		}
		if (!name.equals(EMPTY_NAME))
		{
			selectableJigsawConnectionsToParents.add(
				new SelectableJigsawConnectionToParent(
					new JigsawConnectionToParent(
						jigsaw.info().pos(),
						jigsaw.info().state().getValue(JigsawBlock.ORIENTATION),
						jigsaw.name(),
						jigsaw.placementPriority()),
					jigsaw.selectionPriority()));
		}
	}
}