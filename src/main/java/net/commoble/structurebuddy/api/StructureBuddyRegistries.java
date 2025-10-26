package net.commoble.structurebuddy.api;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Registry keys for StructureBuddy's registries
 */
public final class StructureBuddyRegistries
{
	private StructureBuddyRegistries() {}
	
	// static registries
	/** structurebuddy:dynamic_jigsaw_element_type - static registry for DynamicJigsawElement serializers */
	public static final ResourceKey<Registry<MapCodec<? extends DynamicJigsawElement>>> DYNAMIC_JIGSAW_ELEMENT_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("dynamic_jigsaw_element_type"));
	/** structurebuddy:piece_filler_type - static registry for PieceFiller serializers */
	public static final ResourceKey<Registry<MapCodec<? extends PieceFiller>>> PIECE_FILLER_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("piece_filler"));
	
	// datapack registries
	/** structurebuddy:dynamic_jigsaw_pool - Datapack Registry for dynamic jigsaw pool files which should be placed under data/yourmodid/structurebuddy/dynamic_jigsaw_pool/yourfile.json */
	public static final ResourceKey<Registry<DynamicJigsawPool>> DYNAMIC_JIGSAW_POOL = ResourceKey.createRegistryKey(StructureBuddy.id("dynamic_jigsaw_pool"));
}
