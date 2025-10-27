package net.commoble.structurebuddy.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawElement.DynamicJigsawBakeContext;
import net.commoble.structurebuddy.api.DynamicJigsawPool;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.JigsawConnectionToParent;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructure.DynamicJigsawStructureParams;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructurePiece;
import net.commoble.structurebuddy.api.content.EmptyDynamicJigsawElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.MaxDistance;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Structure placer for dynamic jigsaw structures.
 * Similar to JigsawPlacement but uses an octree implementation for piece fitting instead of VoxelShapes, which makes it run much faster for high iteration levels.
 * @param jigsawPools Registry of DynamicJigsawPools
 * @param maxDepth int maximum iteration depth of jigsaw tree
 * @param chunkGenerator ChunkGenerator
 * @param structureTemplateManager StructureTemplateManager
 * @param pieces List of jigsaw pieces which have been selected and fitted into the jigsaw tree
 * @param random RandomSource suitable for invoking during worldgen
 * @param placingQueue PriorityQueue of potential child-facing connections yet to be evaluated
 */
@ApiStatus.Internal
public record OctreeJigsawPlacer(Registry<DynamicJigsawPool> jigsawPools, int maxDepth, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, List<DynamicJigsawStructurePiece> pieces, RandomSource random, PriorityQueue<OctreePieceState> placingQueue)
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Supplier<List<JigsawConnectionToChild>> NO_AVAILABLE_CONNECTIONS = () -> List.of();
	
	/**
	 * {@return Optional Structure GenerationStub if we can place a structure, empty if we can't}
	 * @param context GenerationContext
	 * @param params DynamicJigsawStructureParams
	 * @param chunkCornerPos BlockPos in absolute world space in the origin column (minimal x/z) of some chunk
	 */
	@ApiStatus.Internal
	public static Optional<Structure.GenerationStub> addPieces(Structure.GenerationContext context, DynamicJigsawStructureParams params, BlockPos chunkCornerPos)
	{
		RegistryAccess registries = context.registryAccess();
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
		LevelHeightAccessor heightAccessor = context.heightAccessor();
		WorldgenRandom rand = context.random();
		Registry<DynamicJigsawPool> templatePools = registries.lookupOrThrow(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL);
		Rotation rotation = Rotation.getRandom(rand);
		var startPoolHolder = params.startPool();
		DynamicJigsawPool startPool = startPoolHolder.value();
		DynamicJigsawElement startElement = startPool.elements().getRandomOrThrow(rand);
		if (startElement == EmptyDynamicJigsawElement.INSTANCE)
		{
			return Optional.empty();
		}
		
		DynamicJigsawResult results = startElement.bake(new DynamicJigsawBakeContext(context, new SubtractiveOctree.NonEmpty(BoundingBox.infinite()), null, NO_AVAILABLE_CONNECTIONS, rotation, params.liquidSettings()));
		PieceFiller pieceFiller = results.pieceFiller();
		BoundingBox startBounds = results.boundingBox(chunkCornerPos);
		List<JigsawConnectionToChild> shuffledJigsawsConnectingToChildren = results.offsetShuffledConnectionsToChildren(chunkCornerPos);
		
		DynamicJigsawStructurePiece startPiece = new DynamicJigsawStructurePiece(
			structureTemplateManager,
			pieceFiller,
			rotation,
			startBounds,
			0,
			params.liquidSettings());
		
		int centerX = (startBounds.maxX() + startBounds.minX()) / 2;
		int centerZ = (startBounds.maxZ() + startBounds.minZ()) / 2;
		int startHeight = params.projectStartToHeightmap()
			.map(heightmap -> chunkCornerPos.getY() + chunkGenerator.getFirstFreeHeight(centerX, centerZ, heightmap, heightAccessor, context.randomState()))
			.orElse(chunkCornerPos.getY());
		
		DimensionPadding dimensionPadding = params.dimensionPadding();
		if (JigsawPlacement.isStartTooCloseToWorldHeightLimits(heightAccessor, dimensionPadding, startBounds))
		{
			LOGGER.debug("Center piece {} with bounding box {} does not fit dimension padding {}", startElement, startBounds, dimensionPadding);
			return Optional.empty();
		}
		
		return Optional.of(new Structure.GenerationStub(new BlockPos(centerX, startHeight, centerZ), builder -> {
			List<DynamicJigsawStructurePiece> pieces = Lists.newArrayList();
			pieces.add(startPiece);
			int maxDepth = params.size();
			if (maxDepth > 0)
			{
				MaxDistance maxDistanceFromCenter = params.maxDistanceFromCenter();
				OctreeJigsawPlacer placer = new OctreeJigsawPlacer(templatePools, maxDepth, chunkGenerator, structureTemplateManager, pieces, rand, new PriorityQueue<>());
				BoundingBox totalBounds = new BoundingBox(
					centerX-maxDistanceFromCenter.horizontal(),
					Math.max(startHeight-maxDistanceFromCenter.vertical(), heightAccessor.getMinY() + dimensionPadding.bottom()),
					centerZ-maxDistanceFromCenter.horizontal(),
					centerX+maxDistanceFromCenter.horizontal(),
					Math.min(startHeight+maxDistanceFromCenter.vertical(), heightAccessor.getMaxY() - dimensionPadding.top()),
					centerZ+maxDistanceFromCenter.horizontal());
				SubtractiveOctree octree = new SubtractiveOctree.NonEmpty(totalBounds);
				boolean totallySubtracted = octree.subtract(startBounds);
				if (totallySubtracted)
				{
					octree = SubtractiveOctree.Empty.INSTANCE;
				}
				int placementCounter = 0;
				placer.placingQueue.add(new OctreePieceState(startPiece, shuffledJigsawsConnectingToChildren, octree, 0, 0, placementCounter++));
				while (!placer.placingQueue.isEmpty())
				{
					OctreePieceState state = placer.placingQueue.remove();
					placementCounter = placer.tryPlacingChildren(context, state, params.liquidSettings(), placementCounter);
				}
				
				pieces.forEach(builder::addPiece);
			}
		}));
	}
	
	private static Predicate<Holder<DynamicJigsawPool>> isValidPool(ResourceKey<DynamicJigsawPool> location)
	{
		return pool -> !pool.value().elements().isEmpty() || location == DynamicJigsawPool.EMPTY;
	}
	
	private int tryPlacingChildren(GenerationContext context, OctreePieceState state, LiquidSettings liquidSettings, int placementCounter)
	{
		SubtractiveOctree totalOctree = state.octree();
		DynamicJigsawStructurePiece parentPiece = state.piece();
		BoundingBox parentBounds = parentPiece.getBoundingBox();
		SubtractiveOctree parentOctree = new SubtractiveOctree.NonEmpty(parentBounds);
		
		forEachJigsaw:
		for (JigsawConnectionToChild parentJigsaw : state.shuffledConnectionsToChildren())
		{
			Direction jigsawFacing = parentJigsaw.orientation().front();
			BlockPos parentJigsawPos = parentJigsaw.pos();
			BlockPos childJigsawPos = parentJigsawPos.relative(jigsawFacing);
			boolean inside = parentBounds.isInside(childJigsawPos);
			SubtractiveOctree permittedSpace = inside
				? parentOctree
				: totalOctree;
			// if the parent is pointing at a position that's no longer available, skip it
			if (!permittedSpace.containsPos(childJigsawPos))
				continue;

			ResourceKey<DynamicJigsawPool> poolKey = parentJigsaw.pool();
			Optional<Reference<DynamicJigsawPool>> maybePool = this.jigsawPools.get(poolKey)
				.filter(isValidPool(poolKey));
			if (maybePool.isEmpty())
			{
				LOGGER.warn("Empty or non-existent pool: {}", poolKey);
				continue;
			}
			Reference<DynamicJigsawPool> poolHolder = maybePool.get();
			DynamicJigsawPool pool = poolHolder.value();
			Holder<DynamicJigsawPool> fallbackPoolHolder = pool.fallback().orElseGet(() -> context.registryAccess().lookupOrThrow(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL).getOrThrow(DynamicJigsawPool.EMPTY));
			ResourceLocation fallbackId = fallbackPoolHolder.unwrapKey().get().location();
			if (!isValidPool(fallbackPoolHolder.getKey()).test(fallbackPoolHolder))
			{
				LOGGER.warn("Empty or non-existent fallback pool: {}", fallbackId);
				continue;
			}
			DynamicJigsawPool fallbackPool = fallbackPoolHolder.value();
			List<DynamicJigsawElement> elements = new ArrayList<>();
			if (state.depth() != this.maxDepth)
			{
				elements.addAll(pool.getShuffledElements(this.random));
			}
			elements.addAll(fallbackPool.getShuffledElements(this.random));
			for (DynamicJigsawElement childElement : elements)
			{
				if (childElement == EmptyDynamicJigsawElement.INSTANCE)
				{
					break;
				}
				// list of remaining connections is expensive to make
				// so only generate it if actually asked for
				// and then we can give the same list to different rotation and jigsaw-of-child attempts
				// once a rotation-and-jigsaw-of-child is chosen, we loop back to the top up there and do not try to bake this piece again
				// so this is the best place to memoize the list
				Supplier<List<JigsawConnectionToChild>> remainingConnections = inside
					? NO_AVAILABLE_CONNECTIONS
					: Suppliers.memoize(() -> {
						List<JigsawConnectionToChild> results = new ArrayList<>();
						for (var remainingState : this.placingQueue)
						{
							for (var remainingJigsaw : remainingState.shuffledConnectionsToChildren())
							{
								BlockPos requiredOpenPos = remainingJigsaw.pos().relative(remainingJigsaw.orientation().front());
								if (permittedSpace.containsPos(requiredOpenPos))
								{
									results.add(remainingJigsaw);
								}
							}
						}
						return results;
					});
				for (Rotation childRotation : Rotation.getShuffled(this.random))
				{
					DynamicJigsawResult childResults = childElement.bake(new DynamicJigsawBakeContext(context, permittedSpace, parentJigsaw, remainingConnections, childRotation, liquidSettings));
					List<JigsawConnectionToParent> localChildJigsaws = childResults.shuffledLocalConnectionsToParent();
					PieceFiller childPieceFiller = childResults.pieceFiller();
					for (JigsawConnectionToParent localChildJigsaw : localChildJigsaws)
					{
						if (parentJigsaw.canAttach(localChildJigsaw))
						{
							BlockPos localChildJigsawPos = localChildJigsaw.pos();
							// if local child pos is 1,0,0 and absolute child pos is 100,64,100
							// then this becomes 99,64,100
							// which is the offset to add to the local child jigsaws and bounds
							BlockPos childPieceOffset = childJigsawPos.subtract(localChildJigsawPos);
							BoundingBox childBounds = childResults.boundingBox(childPieceOffset);
							if (permittedSpace.contains(childBounds))
							{
								permittedSpace.subtract(childBounds);
								int nextDepth = state.depth() + 1;
								DynamicJigsawStructurePiece childPiece = new DynamicJigsawStructurePiece(this.structureTemplateManager, childPieceFiller, childRotation, childBounds, nextDepth, liquidSettings);
								this.pieces.add(childPiece);
								if (nextDepth <= this.maxDepth)
								{
									List<JigsawConnectionToChild> absoluteChildJigsaws = childResults.offsetShuffledConnectionsToChildren(childPieceOffset);
									this.placingQueue.add(new OctreePieceState(childPiece, absoluteChildJigsaws, permittedSpace, nextDepth, localChildJigsaw.placementPriority(), placementCounter++));
								}
								continue forEachJigsaw;
							}
						}
					}
				}
			}
		}
		return placementCounter;
	}
}
