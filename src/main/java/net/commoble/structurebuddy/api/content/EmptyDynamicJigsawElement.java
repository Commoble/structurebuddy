package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Empty DynamicJigsawElement which produces no pieces
 */
public enum EmptyDynamicJigsawElement implements DynamicJigsawElement
{
	/** the singleton instance */
	INSTANCE;

	/** structurebuddy:dynamic_pool_element_type / structurebuddy:empty */
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("empty"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends DynamicJigsawElement>, MapCodec<EmptyDynamicJigsawElement>> HOLDER = DeferredHolder.create(KEY);
	/**
	<pre>
	{
		"type": "structurebuddy:empty"
	}
	</pre>
	 */
	public static final MapCodec<EmptyDynamicJigsawElement> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<? extends DynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		return DynamicJigsawResult.EMPTY;
	}
}
