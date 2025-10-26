package net.commoble.structurebuddy.api;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Can be queried to determine whether a given position or region in an assembling structure is still available
 */
public interface AvailableSpace
{
	/**
	 * Returns true if the region of this space that has NOT been subtracted can contain the given bounding box.
	 * @param newBox A bounding box
	 * @return True if this space's positive space entirely overlaps the given box, false otherwise
	 */
	public abstract boolean contains(BoundingBox newBox);
	
	/**
	 * Returns true if the region of this space that has not been subtracted contains the given position
	 * @param pos Vec3i to check
	 * @return True if this space's positive space contains the given position, false otherwise
	 */
	public abstract boolean containsPos(Vec3i pos);
}
