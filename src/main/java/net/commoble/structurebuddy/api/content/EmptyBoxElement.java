package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.BoxBakeContext;
import net.commoble.structurebuddy.api.BoxElement;
import net.commoble.structurebuddy.api.BoxResult;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * BoxElement which generates nothing in its box
 */
public enum EmptyBoxElement implements BoxElement
{
	/** singleton instance **/
	INSTANCE;

	/** structurebuddy:box_element_type / structurebuddy:empty */
	public static final ResourceKey<MapCodec<? extends BoxElement>> KEY = ResourceKey.create(StructureBuddyRegistries.BOX_ELEMENT_TYPE, StructureBuddy.id("empty"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends BoxElement>, MapCodec<EmptyBoxElement>> HOLDER = DeferredHolder.create(KEY);

	/**
	<pre>
	{
		"type": "structurebuddy:empty"
	}
	</pre>
	 */
	public static final MapCodec<EmptyBoxElement> CODEC = MapCodec.unit(INSTANCE); 

	@Override
	public MapCodec<? extends BoxElement> codec()
	{
		return CODEC;
	}

	@Override
	public BoxResult bake(BoxBakeContext context)
	{
		return BoxResult.empty(context.box());
	}
	
	
}
