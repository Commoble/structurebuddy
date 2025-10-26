package net.commoble.structurebuddy.api.util;

import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Utils for dealing with BoundingBoxes
 */
public final class BoundingBoxUtils
{
	private BoundingBoxUtils() {}
	
	/**
	 * Checks if it is safe to call split on the given box
	 * @param box BoundingBox to split
	 * @param axis Axis to divide the boxes at, e.g. if axis == Axis.X, the box will be split into a left half and a right half
	 * @param secondPieceStart minimum value of second box-half on the given axis
	 * @return true if and only if split(box,axis,secondPieceStart) will not throw an exception
	 */
	public static boolean canSplit(BoundingBox box, Axis axis, int secondPieceStart)
	{
		int relevantMin = switch(axis) {
			case X -> box.minX();
			case Y -> box.minY();
			case Z -> box.minZ();
		};
		int relevantMax = switch(axis) {
			case X -> box.maxX();
			case Y -> box.maxY();
			case Z -> box.maxZ();
		};
		return relevantMax >= secondPieceStart && secondPieceStart > relevantMin;
	}
	
	/**
	 * Splits a bounding box along a given axis such that
	 * @param box BoundingBox to split
	 * @param axis Axis to divide the boxes at, e.g. if axis == Axis.X, the box will be split into a left half and a right half
	 * @param secondPieceStart minimum value of second box-half on the given axis
	 * @return Pair of BoundingBoxes such that
	 * * neither box intersects the other
	 * * the union of the two boxes forms an exact subset of the original box
	 * * the second box's minimal value on the given axis equals secondPieceStart
	 * For example, given a box with minXYZ = 0,0,0, maxXYZ = 10,10,10, splitting on Axis.X at 5,
	 * results in one box with min,max = 0,0,0, 4,10,10
	 * and a second box with min,max = 5,0,0, 10,10,10.
	 * @throws IllegalArgumentException if either box would be empty
	 */
	public static Pair<BoundingBox, BoundingBox> split(BoundingBox box, Axis axis, int secondPieceStart)
	{
		int relevantMin = switch(axis) {
			case X -> box.minX();
			case Y -> box.minY();
			case Z -> box.minZ();
		};
		int relevantMax = switch(axis) {
			case X -> box.maxX();
			case Y -> box.maxY();
			case Z -> box.maxZ();
		};
		if (relevantMax < secondPieceStart || secondPieceStart <= relevantMin)
			throw new IllegalArgumentException("Cannot split " + box + " on axis " + axis + " at value " + secondPieceStart + " as it would result in an empty bounding box");
		int firstMaxX = box.maxX();
		int firstMaxY = box.maxY();
		int firstMaxZ = box.maxZ();
		int secondMinX = box.minX();
		int secondMinY = box.minY();
		int secondMinZ = box.minZ();
		int firstPieceEnd = secondPieceStart-1;
		if (axis == Axis.X)
		{
			firstMaxX = firstPieceEnd;
			secondMinX = secondPieceStart;
		}
		else if (axis == Axis.Y)
		{
			firstMaxY = firstPieceEnd;
			secondMinY = secondPieceStart;
		}
		else // Z
		{
			firstMaxZ = firstPieceEnd;
			secondMinZ = secondPieceStart;
		}
		return Pair.of(
			new BoundingBox(box.minX(), box.minY(), box.minZ(), firstMaxX, firstMaxY, firstMaxZ),
			new BoundingBox(secondMinX, secondMinY, secondMinZ, box.maxX(), box.maxY(), box.maxZ()));
	}
	
	/**
	 * {@return Optional of the intersection between the two given boxes, or empty if they do not intersect}
	 * @param a BoundingBox
	 * @param b another BoundingBox
	 */
	public static Optional<BoundingBox> intersection(BoundingBox a, BoundingBox b)
	{
		return (a.maxX() < b.minX()
			|| b.maxX() < a.minX()
			|| a.maxY() < b.minY()
			|| b.maxY() < a.minY()
			|| a.maxZ() < b.minZ()
			|| b.maxZ() < a.minZ())
		? Optional.empty()
		: Optional.of(new BoundingBox(
			Math.max(a.minX(), b.minX()),
			Math.max(a.minY(), b.minY()),
			Math.max(a.minZ(), b.minZ()),
			Math.min(a.maxX(), b.maxX()),
			Math.min(a.maxY(), b.maxY()),
			Math.min(a.maxZ(), b.maxZ())));
	}
	
	/**
	 * Runs a Consumer on each position in a BoundingBox.
	 * @param box BoundingBox to run a consumer on. Be mindful that bounding boxes are inclusive of their max positions.
	 * @param consumer Consumer to run on each pos in the box. The given blockpos is mutable, so call immutable() on it before storing it anywhere
	 */
	public static void forEachPos(BoundingBox box, Consumer<BlockPos> consumer)
	{
		int minX = box.minX();
		int maxX = box.maxX();
		int minY = box.minY();
		int maxY = box.maxY();
		int minZ = box.minZ();
		int maxZ = box.maxZ();
		MutableBlockPos mutapos = new MutableBlockPos(0,0,0);
		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				for (int z = minZ; z <= maxZ; z++)
				{
					mutapos.set(x,y,z);
					consumer.accept(mutapos);
				}
			}
		}
	}
}
