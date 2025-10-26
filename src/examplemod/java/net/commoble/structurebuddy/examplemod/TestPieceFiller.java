package net.commoble.structurebuddy.examplemod;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.api.util.BoundingBoxUtils;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record TestPieceFiller(BlockState state) implements PieceFiller
{
	public static final ResourceKey<MapCodec<? extends PieceFiller>> KEY = ResourceKey.create(StructureBuddyRegistries.PIECE_FILLER_TYPE, StructureBuddy.id("test"));
	public static final MapCodec<TestPieceFiller> CODEC = BlockState.CODEC.xmap(TestPieceFiller::new, TestPieceFiller::state).fieldOf("state");
	
	@Override
	public MapCodec<? extends PieceFiller> codec()
	{
		return CODEC;
	}

	@Override
	public void fill(DynamicJigsawFillContext context)
	{
		BoundingBox chunkBounds = context.chunkBoundingBox();
		BoundingBox pieceBounds = context.pieceBoundingBox();
		BoundingBox floorBounds = pieceBounds;
		WorldGenLevel level = context.level();
		if (pieceBounds.getYSpan() > 1)
		{
			var floorAndHall = BoundingBoxUtils.split(pieceBounds, Axis.Y, pieceBounds.minY() + 1);
			floorBounds = floorAndHall.getFirst();
			BoundingBox hallBounds = floorAndHall.getSecond();
			BoundingBoxUtils.intersection(hallBounds, chunkBounds).ifPresent(box -> {
				BoundingBoxUtils.forEachPos(box, pos -> {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				});
			});
		}
		BoundingBoxUtils.intersection(floorBounds, chunkBounds).ifPresent(box -> {
			BoundingBoxUtils.forEachPos(box, pos -> {
				level.setBlock(pos, this.state, 2);
			});
		});
	}

}
