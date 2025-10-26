package net.commoble.structurebuddy.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.content.DynamicJigsawStructurePiece;
import net.commoble.structurebuddy.api.util.CodecBuddy;

/**
 * A PieceFiller is serialized inside a {@link DynamicJigsawStructurePiece} when the pieces are stored in region files;
 * the PieceFiller is responsible for placing blocks into generating chunks where they overlap with that piece. 
 */
public interface PieceFiller
{
	/**
	 * Type-dispatched codec for PieceFillers. Sub-codecs can be registered to {@link StructureBuddyRegistries#PIECE_FILLER_TYPE}.
	<pre>
	{
		"type": "yourmod:your_piece_filler",
		// additional fields specified by sub-codec
	}
	</pre>
	 */
	public static final Codec<PieceFiller> CODEC = CodecBuddy.dispatch(StructureBuddyRegistries.PIECE_FILLER_TYPE, PieceFiller::codec);
	
	/** {@return MapCodec for this type of PieceFiller, which has been registered to {@link StructureBuddyRegistries#PIECE_FILLER_TYPE}} */
	public abstract MapCodec<? extends PieceFiller> codec();

	/**
	 * Blocks can be added to the WorldGenLevel here where the given structure piece bounds overlaps with the given chunk bounds in context.
	 * This method is called for each chunk which the piece overlaps,
	 * so is important not to make any RNG calls which can cause inconsistencies across chunk boundaries.
	 * If an RNG call is needed, the provided RandomSource in context should be used.
	 * (randomly deciding which block to place is fine, randomly deciding how wide a section of blocks should be is not as different chunks will disagree).
	 * RNG calls which affect generation spanning multiple chunks should be done in {@link DynamicJigsawElement#bake} instead, and the results stored in this PieceFiller. 
	 * @param context DynamicJigsawFillContext
	 */
	public abstract void fill(DynamicJigsawFillContext context);
}
