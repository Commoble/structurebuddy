package net.commoble.structurebuddy.examplemod;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record TestDynamicJigsawElement(
		HolderSet<Block> blocks,
		ResourceLocation jigsawName,
		ResourceKey<DynamicJigsawPool> targetPool,
		ResourceLocation targetJigsawName
	) implements DynamicJigsawElement
{

	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("test"));
	public static final MapCodec<TestDynamicJigsawElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			HolderSetCodec.create(Registries.BLOCK, BuiltInRegistries.BLOCK.holderByNameCodec(), false).fieldOf("blocks").forGetter(TestDynamicJigsawElement::blocks),
			ResourceLocation.CODEC.fieldOf("jigsaw_name").forGetter(TestDynamicJigsawElement::jigsawName),
			ResourceKey.codec(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL).fieldOf("target_pool").forGetter(TestDynamicJigsawElement::targetPool),
			ResourceLocation.CODEC.fieldOf("target_jigsaw_name").forGetter(TestDynamicJigsawElement::targetJigsawName)
		).apply(builder, TestDynamicJigsawElement::new));
		
	@Override
	public MapCodec<? extends DynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		// get random state for piece filler
		RandomSource random = context.generationContext().random();
		Rotation rotation = context.rotation();
		BlockState state = this.blocks.getRandomElement(random).get().value().defaultBlockState();
		PieceFiller pieceFiller = new TestPieceFiller(state);
		
		int xSize = random.nextIntBetweenInclusive(3, 10);
		int zSize = random.nextIntBetweenInclusive(3, 10);
		int ySize = 4;
		BoundingBox box = new BoundingBox(0,0,0, xSize, ySize, zSize);
		// calculate jigsaws...
		// let's make sure that each rotation guarantees a jigsaw on each side
		List<JigsawConnectionToParent> connectionsToParent = new ArrayList<>();
		List<JigsawConnectionToChild> connectionsToChildren = new ArrayList<>();
		Direction dir = rotation.rotate(Direction.NORTH);
		DynamicJigsawResult.addParentOrChild(
			connectionsToParent,
			connectionsToChildren,
			getJigsawPos(box, dir, random),
			FrontAndTop.fromFrontAndTop(dir, Direction.UP),
			JointType.ALIGNED,
			this.jigsawName,
			this.targetPool,
			this.targetJigsawName,
			JigsawConnectionToParent.DEFAULT_PLACEMENT_PRIORITY);
		// then each of the other sides have a 50% chance of having a jigsaw
		for (int i=0; i<3; i++)
		{
			dir = dir.getClockWise();
			if (random.nextBoolean())
			{
				DynamicJigsawResult.addParentOrChild(
					connectionsToParent,
					connectionsToChildren,
					getJigsawPos(box, dir, random),
					FrontAndTop.fromFrontAndTop(dir, Direction.UP),
					JointType.ALIGNED,
					this.jigsawName,
					this.targetPool,
					this.targetJigsawName,
					JigsawConnectionToParent.DEFAULT_PLACEMENT_PRIORITY);
			}
		}
			
		
		return DynamicJigsawResult.withParentsAndChildren(pieceFiller, box, connectionsToParent, connectionsToChildren);
	}
	
	public BlockPos getJigsawPos(BoundingBox box, Direction dir, RandomSource rand)
	{
		int y = 1;
		int x = switch(dir) {
			case WEST -> box.minX();
			case EAST -> box.maxX();
			default -> rand.nextIntBetweenInclusive(box.minX(), box.maxX());
		};
		int z = switch(dir) {
			case NORTH -> box.minZ();
			case SOUTH -> box.maxZ();
			default -> rand.nextIntBetweenInclusive(box.minZ(), box.maxZ());
		};
		return new BlockPos(x,y,z);
	}

}
