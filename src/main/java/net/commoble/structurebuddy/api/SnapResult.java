package net.commoble.structurebuddy.api;

import java.util.Optional;

import net.commoble.structurebuddy.api.content.StructureTemplateBoxElement;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Result of {@link BoxSnap} determining how to snap a {@link StructureTemplateBoxElement} or similar box element
 * @param x AxisDirection to snap template on x axis, if any
 * @param y AxisDirection to snap template on y axis, if any
 * @param z AxisDirection to snap template on z axis, if any
 */
public record SnapResult(Optional<AxisDirection> x, Optional<AxisDirection> y, Optional<AxisDirection> z)
{
	/** Indicates no snapping is to be done (structure template will be randomly positioned within parent box) **/
	public static final SnapResult NONE = new SnapResult(Optional.empty(), Optional.empty(), Optional.empty());
	
	/**
	 * {@return this SnapResult snapped to the given direction, unless it already snaps on that axis}
	 * @param dir Direction to snap to
	 */
	public SnapResult withDirectionIfNotChosen(Direction dir)
	{
		return switch(dir) {
			case DOWN -> this.withYIfNotChosen(AxisDirection.NEGATIVE);
			case UP -> this.withYIfNotChosen(AxisDirection.POSITIVE); 
			case NORTH -> this.withZIfNotChosen(AxisDirection.NEGATIVE);
			case SOUTH -> this.withZIfNotChosen(AxisDirection.POSITIVE); 
			case WEST -> this.withXIfNotChosen(AxisDirection.NEGATIVE);
			case EAST -> this.withXIfNotChosen(AxisDirection.POSITIVE); 
		};
	}
	
	/**
	 * {@return SnapResult snapped to an x direction, unless it already snaps on x}
	 * @param x AxisDirection to snap x axis to
	 */
	public SnapResult withXIfNotChosen(AxisDirection x)
	{
		return this.x.isPresent()
			? this
			: new SnapResult(Optional.of(x), this.y, this.z);
	}

	/**
	 * {@return SnapResult snapped to a y direction, unless it already snaps on y}
	 * @param y AxisDirection to snap y axis to
	 */
	public SnapResult withYIfNotChosen(AxisDirection y)
	{
		return this.y.isPresent()
			? this
			: new SnapResult(this.x, Optional.of(y), this.z);
	}

	/**
	 * {@return SnapResult snapped to an z direction, unless it already snaps on z}
	 * @param z AxisDirection to snap z axis to
	 */
	public SnapResult withZIfNotChosen(AxisDirection z)
	{
		return this.z.isPresent()
			? this
			: new SnapResult(this.x, this.y, Optional.of(z));
	}
	
	/**
	 * {@return BoundingBox positioned inside parentBox according to this SnapResult's axes, or randomly where not specified}
	 * @param childBox BoundingBox to move inside the parent box. Must be no larger than parentBox.
	 * @param parentBox BoundingBox inside which the child box is moved. Must be no smaller than childBox.
	 * @param random RandomSource to use where random positioning is used
	 */
	public BoundingBox snap(BoundingBox childBox, BoundingBox parentBox, RandomSource random)
	{
		// min amount to move...
		// suppose parent min is 0 and child min is 5
		// we want to add -5 to child
		// which is parent - child
		int minMoveX = parentBox.minX() - childBox.minX();
		int minMoveY = parentBox.minY() - childBox.minY();
		int minMoveZ = parentBox.minZ() - childBox.minZ();
		// max to move...
		// suppose parent max is 10 and child max is 5
		// we want to add 5 to child
		// which is parent - child
		int maxMoveX = parentBox.maxX() - childBox.maxX();
		int maxMoveY = parentBox.maxY() - childBox.maxY();
		int maxMoveZ = parentBox.maxZ() - childBox.maxZ();
		
		int moveX = getAxisSnap(this.x, minMoveX, maxMoveX, random);
		int moveY = getAxisSnap(this.y, minMoveY, maxMoveY, random);
		int moveZ = getAxisSnap(this.z, minMoveZ, maxMoveZ, random);
		
		return childBox.moved(moveX, moveY, moveZ);
	}
	
	private static int getAxisSnap(Optional<AxisDirection> axisDirection, int min, int max, RandomSource random)
	{
		return axisDirection.map(axis -> axis == AxisDirection.POSITIVE
			? max
			: min)
			.orElse(random.nextIntBetweenInclusive(min, max));
	}
}