package net.commoble.structurebuddy.api;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Indicates how a random position should be set along some axis in a finite space
 */
public enum AxisAnchor implements StringRepresentable
{
	/// Centers the position in the middle of a given length (rounding down for even lengths)
	CENTER,
	/// Randomizes the position witin the available length
	RANDOM,
	/// Sets the position at the minimum bounds of the available length
	MIN,
	/// Sets the position at the maximum bounds of the available length
	MAX;
	
	/// ```json
	/// {
	/// 	"some_axis_anchor_in_some_other_serializer": "center"
	/// }
	/// ```
	public static final Codec<AxisAnchor> CODEC = StringRepresentable.fromEnum(AxisAnchor::values);
	
	/// Randomly generates a value between some bounds
	/// @param min Minimum int value
	/// @param max Maximum int value
	/// @param random RandomSource to generate random values where needed
	/// @return int value between min and max according to this anchor type
	public int getValue(int min, int max, RandomSource random)
	{
		return switch(this)
		{
			case CENTER -> (min + max) / 2;
			case RANDOM -> random.nextIntBetweenInclusive(min, max);
			case MIN -> min;
			case MAX -> max;
		};
	}
	
	/// Randomly generates a blockpos within some bounding box
	/// @param box BoundingBox to generate a position within
	/// @param x AxisAnchor to generate the position on the x axis
	/// @param y AxisAnchor to generate the position on the y axis
	/// @param z AxisAnchor to generate the position on the z axis
	/// @param random RandomSource to generate random values where needed
	/// @return BlockPos randomly generated within the given bounding box
	public static BlockPos getPos(BoundingBox box, AxisAnchor x, AxisAnchor y, AxisAnchor z, RandomSource random)
	{
		return new BlockPos(
			x.getValue(box.minX(), box.maxX(), random),
			y.getValue(box.minY(), box.maxY(), random),
			z.getValue(box.minZ(), box.maxZ(), random));
	}

	@Override
	public String getSerializedName()
	{
		return this.name().toLowerCase(Locale.ROOT);
	}
}
