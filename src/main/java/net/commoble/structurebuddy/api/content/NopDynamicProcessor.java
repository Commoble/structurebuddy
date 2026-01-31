package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicProcessor;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/// DynamicProcessor which does nothing
public enum NopDynamicProcessor implements DynamicProcessor
{
	/// singleton instance
	INSTANCE;

	/** structurebuddy:dynamic_structure_processor_type / structurebuddy:nop */
	public static final ResourceKey<MapCodec<? extends DynamicProcessor>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_PROCESSOR_TYPE, StructureBuddy.id("nop"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends DynamicProcessor>, MapCodec<NopDynamicProcessor>> HOLDER = DeferredHolder.create(KEY);
	
	/// ```json
	/// {
	/// 	"type": "structurebuddy:nop"
	/// }
	/// ```
	public static final MapCodec<NopDynamicProcessor> CODEC = MapCodec.unit(INSTANCE);
	
	@Override
	public MapCodec<? extends DynamicProcessor> codec()
	{
		return CODEC;
	}

}
