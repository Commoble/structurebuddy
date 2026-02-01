package net.commoble.structurebuddy.api.content;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.JigsawDataType;
import net.commoble.structurebuddy.api.JigsawPieceDataReader;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * StructurePieces are the thing that gets serialized into region files after structures are assembled so chunks can generate latter on their own schedule
 * This is the StructurePiece impl created by {@link DynamicJigsawStructure}s
 */
public class DynamicJigsawStructurePiece extends StructurePiece implements JigsawPieceDataReader
{
	private static final Logger LOGGER = LogUtils.getLogger();
	
	/** minecraft:worldgen/structure_piece / structurebuddy:dynamic_jigsaw */
	public static final ResourceKey<StructurePieceType> KEY = ResourceKey.create(Registries.STRUCTURE_PIECE, StructureBuddy.id("dynamic_jigsaw"));
	/** holder */
	public static final DeferredHolder<StructurePieceType,StructurePieceType> HOLDER = DeferredHolder.create(KEY);
	
	private final StructureTemplateManager structureTemplateManager;
	private final PieceFiller pieceFiller;
	private final Rotation rotation;
	private final LiquidSettings liquidSettings;
	Map<JigsawDataType<?>,Object> jigsawData;

	/**
	 * Constructor used when creating structure pieces during jigsaw assembly
	 * @param templateManager StructureTemplateManager which has the structure templates
	 * @param pieceFiller PieceFiller determined by {@link DynamicJigsawElement} baking
	 * @param rotation Rotation of this piece
	 * @param elementBox BoundingBox of this piece in absolute world space
	 * @param genDepth int iteration depth (first piece is 0, its children are 1, their children are 2, etc)
	 * @param liquidSettings LiquidSettings describing whether waterlogging in world prior to structure existing should be retained after structure generates
	 * @param jigsawData Map of jigsaw data at the time the piece was selected and saved in the structure piece
	 */
	public DynamicJigsawStructurePiece(
		StructureTemplateManager templateManager,
		PieceFiller pieceFiller,
		Rotation rotation,
		BoundingBox elementBox,
		int genDepth,
		LiquidSettings liquidSettings,
		Map<JigsawDataType<?>,Object> jigsawData)
	{
		super(HOLDER.get(), genDepth, elementBox);
		this.structureTemplateManager = templateManager;
		this.pieceFiller = pieceFiller;
		this.rotation = rotation;
		this.liquidSettings = liquidSettings;
		this.jigsawData = jigsawData;
	}
	
	/**
	 * Constructor used when deserializing structure pieces from region files
	 * @param context context
	 * @param tag CompoundTag of the serialized structure piece
	 */
	public DynamicJigsawStructurePiece(StructurePieceSerializationContext context, CompoundTag tag)
	{
		super(HOLDER.get(),tag);
		this.structureTemplateManager = context.structureTemplateManager();
        DynamicOps<Tag> ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		this.pieceFiller = tag.read("piece_filler", PieceFiller.CODEC, ops).orElseThrow(() -> new IllegalStateException("Invalid piece filler"));
        this.rotation = tag.read("rotation", Rotation.CODEC).orElseThrow();
        this.liquidSettings = tag.read("liquid_settings", LiquidSettings.CODEC).orElse(JigsawStructure.DEFAULT_LIQUID_SETTINGS);
        Map<JigsawDataType<?>, Object> jigsawData = new HashMap<>();
        CompoundTag jigsawDataTag = tag.getCompoundOrEmpty("jigsaw_data");
        for (var key : jigsawDataTag.keySet())
        {
        	Identifier id = Identifier.parse(key);
        	Holder<JigsawDataType<?>> holder = context.registryAccess().lookupOrThrow(StructureBuddyRegistries.JIGSAW_DATA_TYPE).get(id).orElse(null);
        	if (holder == null)
        	{
        		// loading unregistered data probably means a mod was removed or refactored
        		// this shouldn't be fatal
        		LOGGER.debug("Unregistered jigsaw data type {} encountered while loading DynamicJigsawStructurePiece for PieceFiller {} at {}",
        			id,
        			this.pieceFiller,
        			this.boundingBox);
        		continue;
        	}
        	jigsawDataTag.read(key, holder.value().codec(), ops).ifPresent(value -> jigsawData.put(holder.value(), value));
        }
        this.jigsawData = jigsawData;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
	{
        DynamicOps<Tag> ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		tag.store("piece_filler", PieceFiller.CODEC, ops, this.pieceFiller);
        tag.store("rotation", Rotation.CODEC, this.rotation);
        if (this.liquidSettings != JigsawStructure.DEFAULT_LIQUID_SETTINGS) {
            tag.store("liquid_settings", LiquidSettings.CODEC, this.liquidSettings);
        }
        CompoundTag jigsawDataTag = new CompoundTag();
    	var registry = context.registryAccess().lookupOrThrow(StructureBuddyRegistries.JIGSAW_DATA_TYPE);
        for (JigsawDataType<?> type : this.jigsawData.keySet())
        {
        	String id = Objects.requireNonNull(registry.getKey(type), "Unregistered JigsawDataType " + type).toString();
        	this.writeData(context, id, type, jigsawDataTag);
        }
        tag.put("jigsaw_data", jigsawDataTag);
	}
	
	private <T> void writeData(StructurePieceSerializationContext context, String id, JigsawDataType<T> type, CompoundTag tag)
	{
    	T value = this.getData(type);
    	tag.store(id, type.codec(), value);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getData(JigsawDataType<T> type)
	{
		return (T)this.jigsawData.get(type);
	}

	@Override
	public Map<JigsawDataType<?>, Object> toMap()
	{
		return Map.copyOf(this.jigsawData);
	}
	
	@Override
	public void postProcess(
		WorldGenLevel level,
		StructureManager structureManager,
		ChunkGenerator chunkGenerator,
		RandomSource random,
		BoundingBox chunkBoundingBox,
		ChunkPos chunkPos,
		BlockPos startPieceFloorCenter)
	{
		this.pieceFiller.fill(new DynamicJigsawFillContext(level, structureManager, chunkGenerator, random, chunkBoundingBox, chunkPos, startPieceFloorCenter, this.boundingBox, this.structureTemplateManager, this.rotation, this.liquidSettings, this));
	}

}
