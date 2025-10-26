package net.commoble.structurebuddy.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;

/**
 * Represents the state of a jigsaw block (either a real one from a structure template or a fake one from a dynamic jigsaw element) pointing to a child piece
 * @param pos BlockPos of the jigsaw block. May be relative or absolute depending on context.
 * @param orientation FrontAndTop state of the jigsaw.
 * The front is the primary direction of the jigsaw, which must be the opposite of another jigsaw for them to match.
 * The top only matters if front is vertical and jointType is RIGID, in which case tops must be the same for jigsaws to match 
 * @param jointType JointType of the jigsaw. If RIGID and orientation front is vertical, orientation top must be the same for two jigsaws to match.
 * @param pool ResourceKey of the DynamicJigsawPool this parent jigsaw will generate child jigsaws from.
 * @param target ResourceLocation which a child jigsaw must have in order to be targeted by this parent jigsaw   
 */
public record JigsawConnectionToChild(
	BlockPos pos,
	FrontAndTop orientation,
	JointType jointType,
	ResourceKey<DynamicJigsawPool> pool,
	ResourceLocation target)
{	
	/**
	 * {@return JigsawConnectionToChild with same values as this except moved by the given offset}
	 * @param offset Vec3i to offset this connection by
	 */
	public JigsawConnectionToChild moved(Vec3i offset)
	{
		return new JigsawConnectionToChild(
			this.pos.offset(offset),
			this.orientation,
			this.jointType,
			this.pool,
			this.target);
	}
	
	/**
	 * {@return true if this connection can attach to the given child connection, false otherwise}
	 * @param child JigsawConnectionToParent (i.e. a connection in a trying piece trying to connect to this child-facing connection in a parent piece)
	 */
	public boolean canAttach(JigsawConnectionToParent child)
	{
		FrontAndTop parentOrientation = this.orientation();
		FrontAndTop childOrientation = child.orientation();
		Direction parentFront = parentOrientation.front();
		Direction childFront = childOrientation.front();
		Direction parentTop = parentOrientation.top();
		Direction childTop = childOrientation.top();
		JigsawBlockEntity.JointType parentJointType = this.jointType();
		boolean isParentRollable = parentJointType == JigsawBlockEntity.JointType.ROLLABLE;
		return parentFront == childFront.getOpposite() && (isParentRollable || parentTop == childTop) && this.target().equals(child.name());
	}
}
