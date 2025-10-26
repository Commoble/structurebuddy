package net.commoble.structurebuddy.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.resources.ResourceLocation;
/**
 * Represents the state of a jigsaw connection pointing to the parent of some child jigsaw piece.
 * @param pos BlockPos of the jigsaw block. May be relative or absolute depending on context.
 * @param orientation FrontAndTop state of the jigsaw.
 * The front is the primary direction of the jigsaw, which must be the opposite of the parent jigsaw for them to match
 * The top only matters if front is vertical and parent has JointType RIGID, in which case the tops must be the same to connect
 * @param name ResourceLocation of this jigsaw when used as a child connector, to be targeted by parent jigsaws 
 * @param placementPriority int designating when this jigsaw's child piece should be processed, after being selected, relative to other pieces. Higher = sooner.
 * The child piece has already been selected and oriented within the jigsaw tree once this is used, so this causes the children of the child to be checked for fitting earlier, not the child itself.
 */ 
public record JigsawConnectionToParent(
	BlockPos pos,
	FrontAndTop orientation,
	ResourceLocation name,
	int placementPriority)
{
	/** Default placementPriority for jigsaw connections **/
	public static final int DEFAULT_PLACEMENT_PRIORITY = 0;
	
	/**
	 * Constructs a JigsawConnectionToParent with a default placement priority
	 * @param pos BlockPos of the jigsaw block. May be relative or absolute depending on context.
	 * @param orientation FrontAndTop state of the jigsaw.
	 * The front is the primary direction of the jigsaw, which must be the opposite of the parent jigsaw for them to match
	 * The top only matters if front is vertical and parent has JointType RIGID, in which case the tops must be the same to connect
	 * @param name ResourceLocation of this jigsaw when used as a child connector, to be targeted by parent jigsaws 
	 */
	public JigsawConnectionToParent(BlockPos pos, FrontAndTop orientation, ResourceLocation name)
	{
		this(pos, orientation, name, DEFAULT_PLACEMENT_PRIORITY);
	}
}
