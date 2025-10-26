package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.DynamicOps;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
public class DynamicJigsawStructurePiece extends StructurePiece
{
	/** minecraft:worldgen/structure_piece / structurebuddy:dynamic_jigsaw */
	public static final ResourceKey<StructurePieceType> KEY = ResourceKey.create(Registries.STRUCTURE_PIECE, StructureBuddy.id("dynamic_jigsaw"));
	/** holder */
	public static final DeferredHolder<StructurePieceType,StructurePieceType> HOLDER = DeferredHolder.create(KEY);
	
	private final StructureTemplateManager structureTemplateManager;
	private final PieceFiller pieceFiller;
	private final Rotation rotation;
	private final LiquidSettings liquidSettings;

	/**
	 * Constructor used when creating structure pieces during jigsaw assembly
	 * @param templateManager StructureTemplateManager which has the structure templates
	 * @param pieceFiller PieceFiller determined by {@link DynamicJigsawElement} baking
	 * @param rotation Rotation of this piece
	 * @param elementBox BoundingBox of this piece in absolute world space
	 * @param genDepth int iteration depth (first piece is 0, its children are 1, their children are 2, etc)
	 * @param liquidSettings LiquidSettings describing whether waterlogging in world prior to structure existing should be retained after structure generates
	 */
	public DynamicJigsawStructurePiece(
		StructureTemplateManager templateManager,
		PieceFiller pieceFiller,
		Rotation rotation,
		BoundingBox elementBox,
		int genDepth,
		LiquidSettings liquidSettings)
	{
		super(HOLDER.get(), genDepth, elementBox);
		this.structureTemplateManager = templateManager;
		this.pieceFiller = pieceFiller;
		this.rotation = rotation;
		this.liquidSettings = liquidSettings;
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
		this.pieceFiller.fill(new DynamicJigsawFillContext(level, structureManager, chunkGenerator, random, chunkBoundingBox, chunkPos, startPieceFloorCenter, this.boundingBox, this.structureTemplateManager, this.rotation, this.liquidSettings));
	}

}
