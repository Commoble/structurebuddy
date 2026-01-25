package net.commoble.structurebuddy.api;

import java.util.function.Consumer;

import org.apache.commons.lang3.function.Consumers;

import net.commoble.structurebuddy.api.content.EmptyPieceFiller;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Result of baking a {@link BoxElement} into a {@link BoundingBox} of a known size.
 * @param pieceFiller PieceFiller which will be serialized in the StructurePiece in region files,
 * and used later to fill the piece when overlapping chunks generate.
 * @param localBoundingBox BoundingBox to be provided to {@link PieceFiller#fill}.
 * Must be relative to the coordinates of the box provided by {@link BoxElement#bake}.
 * Consumers of the DynamicBoxResult should refuse to generate anything if localBoundingBox is not contained by the
 * box provided by the context (and elements may return {@link BoxResult#invalid} to indicate an invalid result).
 * @param onSelected Consumer to apply modifications to shared jigsaw piece data,
 * which will run if these results are selected to add a piece to the structure.
 */
public record BoxResult(
	PieceFiller pieceFiller,
	BoundingBox localBoundingBox,
	Consumer<JigsawDataAccess> onSelected)
{
	/**
	 * Creates a DynamicBoxResult indicating that nothing should be generated in the given bounds.
	 * Semantically different from {@link BoxResult#invalid} in ways that are important for things which iterate over a
	 * list of elements until a valid element is found;
	 * empty indicates that the element may be used but chooses to generate nothing,
	 * while invalid indicates that the element refuses to generate anything and should not be used for the given context
	 * (perhaps it can't fit into the given space).
	 * @param box BoundingBox from {@link BoxBakeContext} which this element is baking into
	 * @return DynamicBoxResult indicating this element will generate nothing
	 */
	public static BoxResult empty(BoundingBox box)
	{
		return new BoxResult(
			EmptyPieceFiller.INSTANCE,
			box,
			Consumers.nop());
	}
	
	/**
	 * Creates a DynamicBoxResult indicating nothing can be generated in the given bounds.
	 * Semantically different from {@link BoxResult#empty} in ways that are important for things which iterate over a
	 * list of elements until a valid element is found;
	 * empty indicates that the element may be used but chooses to generate nothing,
	 * while invalid indicates that the element refuses to generate anything and should not be used for the given context
	 * (perhaps it can't fit into the given space).
	 * @return DynamicBoxResult indicating this result should not be used
	 */
	public static BoxResult invalid()
	{
		return new BoxResult(
			EmptyPieceFiller.INSTANCE,
			BoundingBox.infinite(),
			Consumers.nop());
	}
}
