package net.commoble.structurebuddy.api;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Context given to {@link DynamicJigsawElement#bake}
 * @param generationContext GenerationContext
 * @param remainingSpace OctreeView of the remaining space available where this child piece can generate. Absolute world coordinates.
 * If this is child is generating within the interior of another piece, this will be restricted to the bounds of that parent piece.
 * Please do not attempt to mutate this as it will be reused across multiple generation attempts and your piece may not be selected.
 * For the start piece, a space of infinite size is provided as the actual space is calculated after the first piece forms,
 * so be mindful that the given space may be larger than actual space available.
 * For pieces of fixed size, it is generally not necessary to use this to verify that your piece will fit into the available space
 * as this is done by the jigsaw assembler anyway;
 * the purpose of this is more for shaping dynamic pieces into the available space
 * @param parent Jigsaw information of the parent jigsaw (including pos and orientation). Absolute world coordinates.
 * Will be null if this is the start piece.
 * (the connection of the parent points to this jigsaw piece, which is the child)
 * @param remainingConnections Jigsaw connections which are yet to be processed.
 * Interior connections are ignored,
 * and list will be empty if this piece is an interior piece.
 * Can be used to attempt to loop back onto an existing piece.
 * @param data JigsawDataReader providing read access to data shared by other pieces selected to be placed into the tree.
 * Data retrieved from the reader should not be modified or mutated as your element has not yet been selected for placing.
 * @param rotation Rotation
 * @param liquidSettings LiquidSettings
 */
public record DynamicJigsawBakeContext(
	GenerationContext generationContext,
	AvailableSpace remainingSpace,
	@Nullable JigsawConnectionToChild parent,
	Supplier<List<JigsawConnectionToChild>> remainingConnections,
	JigsawDataReader data,
	Rotation rotation,
	LiquidSettings liquidSettings)
{
}