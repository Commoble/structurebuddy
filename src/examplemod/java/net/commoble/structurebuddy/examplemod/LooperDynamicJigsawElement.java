package net.commoble.structurebuddy.examplemod;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.DynamicJigsawPool;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.JigsawConnectionToParent;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record LooperDynamicJigsawElement(
	double loopChance,
	double turnChance,
	int minLength,
	int maxLength,
	int maxWidth,
	HolderSet<Block> blocks,
	ResourceLocation jigsawName,
	ResourceKey<DynamicJigsawPool> targetPool,
	ResourceLocation targetJigsawName
	) implements DynamicJigsawElement
{
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("looper"));
	public static final MapCodec<LooperDynamicJigsawElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Codec.DOUBLE.fieldOf("loop_chance").forGetter(LooperDynamicJigsawElement::loopChance),
			Codec.DOUBLE.fieldOf("turn_chance").forGetter(LooperDynamicJigsawElement::turnChance),
			Codec.INT.fieldOf("min_length").forGetter(LooperDynamicJigsawElement::minLength),
			Codec.INT.fieldOf("max_length").forGetter(LooperDynamicJigsawElement::maxLength),
			Codec.INT.fieldOf("max_width").forGetter(LooperDynamicJigsawElement::maxWidth),
			HolderSetCodec.create(Registries.BLOCK, BuiltInRegistries.BLOCK.holderByNameCodec(), false).fieldOf("blocks").forGetter(LooperDynamicJigsawElement::blocks),
			ResourceLocation.CODEC.fieldOf("jigsaw_name").forGetter(LooperDynamicJigsawElement::jigsawName),
			ResourceKey.codec(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL).fieldOf("target_pool").forGetter(LooperDynamicJigsawElement::targetPool),
			ResourceLocation.CODEC.fieldOf("target_jigsaw_name").forGetter(LooperDynamicJigsawElement::targetJigsawName)
		).apply(builder, LooperDynamicJigsawElement::new));
	
	@Override
	public MapCodec<? extends DynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		RandomSource random = context.generationContext().random();
		Rotation rotation = context.rotation();
		boolean tryLoop = context.parent() == null ? false : random.nextDouble() < this.loopChance;
		boolean turning = random.nextDouble() < this.turnChance;
		int length = random.nextIntBetweenInclusive(this.minLength, this.maxLength);
		int width = turning ? random.nextIntBetweenInclusive(1, this.maxWidth) : 0;
		@Nullable JigsawConnectionToChild parentJigsaw = context.parent();
		Direction directionToParent;
		BlockPos startPos;
		if (parentJigsaw == null)
		{
			directionToParent = rotation.rotate(Direction.NORTH);
			startPos = BlockPos.ZERO;
		}
		else
		{
			Direction parentDir = parentJigsaw.orientation().front();
			directionToParent = parentDir.getOpposite();
			startPos = parentJigsaw.pos().relative(parentDir);
		}
		
		Direction startDir = directionToParent.getOpposite();
		Direction endDir = turning && maxWidth != 0
			? random.nextInt(3)==0 ? Rotation.CLOCKWISE_90.rotate(startDir) : Rotation.COUNTERCLOCKWISE_90.rotate(startDir)
			: startDir;
		if (tryLoop)
		{
			BlockPos maxEndPos = startPos.relative(startDir, maxLength).relative(endDir, maxWidth);
			BoundingBox maxBounds = BoundingBox.fromCorners(startPos, maxEndPos);
			for (JigsawConnectionToChild targetJigsaw : context.remainingConnections().get())
			{
				FrontAndTop targetOrientation = targetJigsaw.orientation();
				Direction targetFacing = targetOrientation.front();
				if (targetFacing != startDir)
				{
					endDir = targetFacing.getOpposite();
					if (startDir == endDir)
						width = 0;
					BlockPos endPos = targetJigsaw.pos().relative(targetFacing);
					if (maxBounds.isInside(endPos))
					{
						BoundingBox requiredBounds = BoundingBox.fromCorners(startPos, endPos);
						if (context.remainingSpace().contains(requiredBounds))
						{
							int actualLength = startDir.getAxis() == Direction.Axis.X
								? Math.abs(endPos.getX() - startPos.getX()) + 1
								: Math.abs(endPos.getZ() - startPos.getZ()) + 1;
							int actualWidth = endDir.getAxis() == Direction.Axis.X
								? Math.abs(endPos.getX() - startPos.getX())
								: Math.abs(endPos.getZ() - startPos.getZ());
							if (width == 0 && actualWidth != 0)
								continue;
							// encode startpos relative to piece minima
							BlockPos pieceMinima = new BlockPos(requiredBounds.minX(), requiredBounds.minY(), requiredBounds.minZ());
							BlockPos encodedStartPos = startPos.subtract(pieceMinima);
							PieceFiller pieceFiller = new LooperPieceFiller(encodedStartPos, startDir, endDir, actualLength, actualWidth, this.blocks.getRandomElement(random).get().value());
							List<JigsawConnectionToParent> jigsaws = List.of(
								new JigsawConnectionToParent(
									startPos,
									FrontAndTop.fromFrontAndTop(directionToParent, Direction.UP),
									this.jigsawName));
							// only need the start jigsaw, not the end
							// because we seal off the child connection by placing our box there
							return DynamicJigsawResult.withParents(pieceFiller, requiredBounds, jigsaws);
						}
					}
				}
			}
		}
		BlockPos endPos = startPos.relative(startDir, length-1).relative(endDir, width);
		BoundingBox boundingBox = BoundingBox.fromCorners(startPos, endPos);
		// encode startpos relative to piece minima
		BlockPos pieceMinima = new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());
		BlockPos encodedStartPos = startPos.subtract(pieceMinima);
		PieceFiller pieceFiller = new LooperPieceFiller(encodedStartPos, startDir, endDir, length, width, this.blocks.getRandomElement(random).get().value());
		JigsawConnectionToParent connectionToParent = new JigsawConnectionToParent(
				startPos,
				FrontAndTop.fromFrontAndTop(directionToParent, Direction.UP),
				this.jigsawName);
		JigsawConnectionToChild connectionToChild = new JigsawConnectionToChild(
				endPos,
				FrontAndTop.fromFrontAndTop(endDir, Direction.UP),
				JointType.ALIGNED,
				this.targetPool,
				this.targetJigsawName);
		return DynamicJigsawResult.withParentsAndChildren(pieceFiller, boundingBox, List.of(connectionToParent), List.of(connectionToChild));
	}

	public static record LooperPieceFiller(BlockPos startPos, Direction startDir, Direction endDir, int length, int width, Block block) implements PieceFiller
	{
		public static final ResourceKey<MapCodec<? extends PieceFiller>> KEY = ResourceKey.create(StructureBuddyRegistries.PIECE_FILLER_TYPE, StructureBuddy.id("looper"));
		public static final MapCodec<LooperPieceFiller> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
				BlockPos.CODEC.fieldOf("start_pos").forGetter(LooperPieceFiller::startPos),
				Direction.CODEC.fieldOf("start_dir").forGetter(LooperPieceFiller::startDir),
				Direction.CODEC.fieldOf("end_dir").forGetter(LooperPieceFiller::endDir),
				Codec.INT.fieldOf("length").forGetter(LooperPieceFiller::length),
				Codec.INT.fieldOf("width").forGetter(LooperPieceFiller::width),
				BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(LooperPieceFiller::block)
			).apply(builder, LooperPieceFiller::new));
		
		@Override
		public MapCodec<? extends PieceFiller> codec()
		{
			return CODEC;
		}

		@Override
		public void fill(DynamicJigsawFillContext context)
		{
			BoundingBox chunk = context.chunkBoundingBox();
			WorldGenLevel level = context.level();
			BlockState state = block.defaultBlockState();
			BoundingBox pieceBounds = context.pieceBoundingBox();
			BlockPos pieceMinima = new BlockPos(pieceBounds.minX(), pieceBounds.minY(), pieceBounds.minZ());
			// encoded startpos is relative to piece minima
			BlockPos startPos = pieceMinima.offset(this.startPos);
			for (int i=0; i<length; i++)
			{
				BlockPos pos = startPos.relative(startDir, i);
				if (chunk.isInside(pos))
				{
					level.setBlock(pos, i == 0 ? Blocks.COBBLESTONE.defaultBlockState() : state, 2);
				}
			}
			for (int i=0; i<width; i++)
			{
				BlockPos pos = startPos.relative(startDir, length-1).relative(endDir, i+1); 
				if (chunk.isInside(pos))
				{
					level.setBlock(pos, state, 2);
				}
			}
		}
		
	}
}
