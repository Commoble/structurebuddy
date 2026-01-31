package net.commoble.structurebuddy.api.content;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicProcessor;
import net.commoble.structurebuddy.api.JigsawPieceDataReader;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/// DynamicProcessor which delegates to a vanilla processor list
/// @param processors Holder referencing a structure processor list file (data/namespace/worldgen/processor_list/path.json)
public record ProcessorListDynamicProcessor(Holder<StructureProcessorList> processors) implements DynamicProcessor
{

	/** minecraft:worldgen/structure_processor / structurebuddy:dynamic_structure_processor_wrapper **/
	public static final ResourceKey<MapCodec<? extends DynamicProcessor>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_PROCESSOR_TYPE, StructureBuddy.id("processor_list_dynamic_processor"));
	/** holder **/
	public static final DeferredHolder<MapCodec<? extends DynamicProcessor>, MapCodec<ProcessorListDynamicProcessor>> HOLDER = DeferredHolder.create(KEY);
	
	public static final MapCodec<ProcessorListDynamicProcessor> CODEC = StructureProcessorType.LIST_CODEC
		.xmap(ProcessorListDynamicProcessor::new, ProcessorListDynamicProcessor::processors)
		.fieldOf("processors");
	
	@Override
	public MapCodec<? extends DynamicProcessor> codec()
	{
		return CODEC;
	}

	@Override
	public @Nullable StructureBlockInfo process(
		LevelReader level,
		BlockPos targetPosition,
		BlockPos referencePos,
		StructureBlockInfo originalBlockInfo,
		StructureBlockInfo processedBlockInfo,
		StructurePlaceSettings settings,
		@Nullable StructureTemplate template,
		JigsawPieceDataReader jigsawData)
	{
		@Nullable StructureBlockInfo modified = processedBlockInfo;
		for (var processor : this.processors.value().list())
		{
			modified = processor.process(level, targetPosition, referencePos, originalBlockInfo, processedBlockInfo, settings, template);
			if (modified == null)
				break;
		}
		return modified;
	}

	@Override
	public List<StructureBlockInfo> finalizeProcessing(
		ServerLevelAccessor level,
		BlockPos position,
		BlockPos referencePos,
		List<StructureBlockInfo> originalBlockInfoList,
		List<StructureBlockInfo> processedBlockInfoList,
		StructurePlaceSettings settings,
		JigsawPieceDataReader jigsawData)
	{
		for (var processor : this.processors.value().list())
		{
			processedBlockInfoList = processor.finalizeProcessing(level, position, referencePos, originalBlockInfoList, processedBlockInfoList, settings);
		}
		return processedBlockInfoList;
	}

	// neoforge api spec is wrong, StructureProcessor#processEntity can and does accept nullable StructureTemplates
	// and can and does return nullable entity info
	@SuppressWarnings({ "null", "unused" })
	@Override
	public @Nullable StructureEntityInfo processEntity(
		LevelReader world,
		BlockPos seedPos,
		StructureEntityInfo rawEntityInfo, 
		StructureEntityInfo entityInfo,
		StructurePlaceSettings placementSettings,
		@Nullable StructureTemplate template,
		JigsawPieceDataReader jigsawData)
	{
		@Nullable StructureEntityInfo modified = entityInfo;
		for (var processor : this.processors.value().list())
		{
			modified = processor.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
			if (modified == null)
				break;
		}
		return modified;
	}

	
}
