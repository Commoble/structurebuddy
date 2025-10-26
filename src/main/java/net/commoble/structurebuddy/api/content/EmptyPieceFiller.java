package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Empty PieceFiller indicating nothing is to be placed into a structure piece, or that there is no structure piece to place blocks into
 */
public enum EmptyPieceFiller implements PieceFiller
{
	/** the singleton instance */
	INSTANCE;
	
	/** structurebuddy:piece_filler_type / structurebuddy:empty */
	public static final ResourceKey<MapCodec<? extends PieceFiller>> KEY = ResourceKey.create(StructureBuddyRegistries.PIECE_FILLER_TYPE, StructureBuddy.id("empty"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends PieceFiller>, MapCodec<EmptyPieceFiller>> HOLDER = DeferredHolder.create(KEY);
	/**
	<pre>
	{
		"type": "structurebuddy:empty"
	}
	</pre>
	 */
	public static final MapCodec<EmptyPieceFiller> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends PieceFiller> codec()
	{
		return CODEC;
	}

	@Override
	public void fill(DynamicJigsawFillContext context)
	{
	}
}
