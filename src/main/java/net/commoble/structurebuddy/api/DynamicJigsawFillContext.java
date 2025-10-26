package net.commoble.structurebuddy.api;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Context provided to {@link PieceFiller#fill}
 * @param level WorldGenLevel the structure is placing blocks into.
 * @param structureManager StructureManager
 * @param chunkGenerator ChunkGenerator
 * @param random RandomSource which is safe to use for world generation.
 * @param chunkBoundingBox BoundingBox of the chunk currently being generated. Blocks should not be placed outside of these bounds.
 * @param chunkPos ChunkPos of the chunk being generated.
 * @param startPieceFloorCenter BlockPos at the center of the bottom of the start piece
 * @param pieceBoundingBox BoundingBox of the structure piece. The intersection of this box and the chunkBoundingBox is where blocks should be placed.
 * @param structureTemplateManager StructureTemplateManager holding structure nbt templates.
 * @param rotation Rotation of the structure piece
 * @param liquidSettings LiquidSettings indicating whether to preserve waterlogging from world into placed blocks
 */
public record DynamicJigsawFillContext(
	WorldGenLevel level,
	StructureManager structureManager,
	ChunkGenerator chunkGenerator,
	RandomSource random,
	BoundingBox chunkBoundingBox,
	ChunkPos chunkPos,
	BlockPos startPieceFloorCenter,
	BoundingBox pieceBoundingBox,
	StructureTemplateManager structureTemplateManager,
	Rotation rotation,
	LiquidSettings liquidSettings) {
	
}
