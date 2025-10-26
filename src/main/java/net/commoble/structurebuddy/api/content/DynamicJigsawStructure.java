package net.commoble.structurebuddy.api.content;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawPool;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.internal.OctreeJigsawPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Structure type similar to vanilla template pool jigsaw structures except
 * * Jigsaw assembling uses an octree-based implementation instead of VoxelShapes, which runs much faster
 * * Supports randomizable structure piece sizes
 * * Provides additional information about the current state of the generated structure to pool elements, such as remaining permitted space and available unused connections
 * * Lacks some vanilla features such as terrain matching and the village expansion hack
 */
public class DynamicJigsawStructure extends Structure
{
	// structure should be an interface but it isn't so we can't make our subclass a record
	// too lazy to write getters for each of these
	/**
	 * Params parsed from DynamicJigsawStructure jsons
	 * @param startPool Holder of a DynamicJigsawPool which the structure will use to generate the first piece.
	 * @param size int maximum iteration depth of the jigsaw assembler (how deep to generate children of children of children of etc.).
	 * 7 is common for vanilla structures but can be bigger due to our more efficient algorithm.
	 * @param startHeight HeightProvider determining where to place the floor of the first piece. Ignored if projectStartToHeightmap is present.
	 * @param projectStartToHeightmap Optional HeightMap; if present, floor of first piece is placed at given heightmap and startHeight is ignored.
	 * @param maxDistanceFromCenter int radius of the cuboid which the entire structure tree can generate within.
	 * @param dimensionPadding DimensionPadding indicating the minimum distance the structure can generate from the top or bottom of the world.
	 * @param liquidSettings LiquidSettings indicating whether the structure's generated blocks should inherit waterloggedness from existing blocks in the world.
	 */
	public static record DynamicJigsawStructureParams(
		Holder<DynamicJigsawPool> startPool,
		int size,
		HeightProvider startHeight,
		Optional<Heightmap.Types> projectStartToHeightmap,
		int maxDistanceFromCenter,
//		List<PoolAliasBinding> poolAliases, // not currently used
		DimensionPadding dimensionPadding,
		LiquidSettings liquidSettings) {}
	
	/** minecraft:worldgen/structure_type / structurebuddy:dynamic_jigsaw **/
	public static final ResourceKey<StructureType<?>> KEY = ResourceKey.create(Registries.STRUCTURE_TYPE, StructureBuddy.id("dynamic_jigsaw"));
	/** holder **/
	public static final DeferredHolder<StructureType<?>,StructureType<DynamicJigsawStructure>> HOLDER = DeferredHolder.create(KEY);
	
	/**
	 * Codec of just the params specific to this structure type
	 * @see DynamicJigsawStructure#CODEC for example json including additional params common to all structures
	 **/
	public static final MapCodec<DynamicJigsawStructureParams> PARAMS_CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			DynamicJigsawPool.CODEC.fieldOf("start_pool").forGetter(DynamicJigsawStructureParams::startPool),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("size").forGetter(DynamicJigsawStructureParams::size),
			HeightProvider.CODEC.fieldOf("start_height").forGetter(DynamicJigsawStructureParams::startHeight),
			Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(DynamicJigsawStructureParams::projectStartToHeightmap),
			Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(DynamicJigsawStructureParams::maxDistanceFromCenter),
//			Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(DynamicJigsawStructureParams::poolAliases),
			DimensionPadding.CODEC.optionalFieldOf("dimension_padding", DimensionPadding.ZERO).forGetter(DynamicJigsawStructureParams::dimensionPadding),
			LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.IGNORE_WATERLOGGING).forGetter(DynamicJigsawStructureParams::liquidSettings))
		.apply(builder, DynamicJigsawStructureParams::new));
	
	/**
	 * e.g.
	 * {
		<pre>
		{
			"type": "structurebuddy:dynamic_jigsaw",
			"biomes": "#is_forest", // which biomes the structure can spawn in
			"spawn_overrides": {} // for any mob categories present, provided mob categories will spawn mobs from this configuration instead of the biome's; for format see https://minecraft.wiki/w/Structure_definition
			"step": "surface_structures", // or "underground_structures", but can be any {@link GenerationStep}
			"terrain_adaptation": "beard_thin", // optional, defaults to none, terraforms terrain around the structure if present
			"size": 7, // Iteration depth of jigsaw tree. 7 is common vanilla default. If 0, only fallback pools will generate. More size = structure takes longer to generate
			"start_height": 0, // ignored if project_start_to_hightmap is used. See HeightProvider codec if this is to be used
			"project_start_to_heightmap": "WORLD_SURFACE_WG", // if present, starts structure at that heightmap and ignores start_height
			"max_distance_from_center": 80, // range of [1, 128], vanilla commonly uses 80 or 116 in jigsaw structures
			"dimension_paddIng": 5, // optional, defaults to zero if not present; prevents structure from spawning too close to top/bottom of world
			"liquid_settings": "apply_waterlogging" // optional, defaults to "ignore_waterlogging" if not present
		}
		</pre>
	 * }
	 */
	public static final MapCodec<DynamicJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Structure.settingsCodec(builder),
			PARAMS_CODEC.forGetter(DynamicJigsawStructure::params)
	    ).apply(builder, DynamicJigsawStructure::new));
	
	private final DynamicJigsawStructureParams params;
	
	/** {@return params} */
	public DynamicJigsawStructureParams params()
	{
		return this.params;
	}

	/**
	 * constructor
	 * @param settings StructureSettings common to all structures
	 * @param params DynamicJigsawStructureParams specific to this structure
	 */
	public DynamicJigsawStructure(StructureSettings settings, DynamicJigsawStructureParams params)
	{
		super(settings);
		this.params = params;
	}

	@Override
	public StructureType<?> type()
	{
		return HOLDER.get();
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context)
	{
		ChunkPos chunkPos = context.chunkPos();
		int startY = this.params.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
		BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), startY, chunkPos.getMinBlockZ());
		return OctreeJigsawPlacer.addPieces(context, this.params, startPos);
	}
}
