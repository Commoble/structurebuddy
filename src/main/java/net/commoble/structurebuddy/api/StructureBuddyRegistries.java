package net.commoble.structurebuddy.api;

import java.util.List;

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
	/** structurebuddy:box_element_type - static registry for BoxElement serializers */
	public static final ResourceKey<Registry<MapCodec<? extends BoxElement>>> BOX_ELEMENT_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("box_element_type"));
	/** structurebuddy:dynamic_jigsaw_element_type - static registry for DynamicJigsawElement serializers */
	public static final ResourceKey<Registry<MapCodec<? extends DynamicJigsawElement>>> DYNAMIC_JIGSAW_ELEMENT_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("dynamic_jigsaw_element_type"));
	/** structurebuddy:dynamic_processor_type - static registry for DynamicProcessor serializers */
	public static final ResourceKey<Registry<MapCodec<? extends DynamicProcessor>>> DYNAMIC_PROCESSOR_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("dynamic_processor_type"));
	/** structurebuddy:piece_filler_type - static registry for PieceFiller serializers */
	public static final ResourceKey<Registry<MapCodec<? extends PieceFiller>>> PIECE_FILLER_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("piece_filler_type"));
	/** structurebuddy:jigsaw_data_ttype - static registry for jigsaw data serializers */
	public static final ResourceKey<Registry<JigsawDataType<?>>> JIGSAW_DATA_TYPE = ResourceKey.createRegistryKey(StructureBuddy.id("jigsaw_data_type"));
	
	// datapack registries
	/** structurebuddy:box_pool - Datapack Registry for box pool files which should be placed under data/yourmodid/structurebuddy/box_pool/yourfile.json */
	public static final ResourceKey<Registry<BoxPool>> BOX_POOL = ResourceKey.createRegistryKey(StructureBuddy.id("box_pool"));
	/** structurebuddy:dynamic_jigsaw_pool - Datapack Registry for dynamic jigsaw pool files which should be placed under data/yourmodid/structurebuddy/dynamic_jigsaw_pool/yourfile.json */
	public static final ResourceKey<Registry<DynamicJigsawPool>> DYNAMIC_JIGSAW_POOL = ResourceKey.createRegistryKey(StructureBuddy.id("dynamic_jigsaw_pool"));
	/** structurebuddy:dynamic_processor_list - Datapack Registry for lists of dynamic structure processors which should be placed under data/yourmodid/structurebuddy/dynamic_processor_list/yourfile.json */
	public static final ResourceKey<Registry<List<DynamicProcessor>>> DYNAMIC_PROCESSOR_LIST = ResourceKey.createRegistryKey(StructureBuddy.id("dynamic_processor_list"));
}
