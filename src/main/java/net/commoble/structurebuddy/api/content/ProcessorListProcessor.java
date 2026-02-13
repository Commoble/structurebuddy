package net.commoble.structurebuddy.api.content;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.StructureBuddy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/// StructureProcessor which delegates to another processor list
public class ProcessorListProcessor extends StructureProcessor
{
	/// ```json
	/// {
	/// 	"processor_type": "structurebuddy:processor_list",
	/// 	"processors": "modid:another_processor_list"
	/// }
	/// ```
	public static final MapCodec<ProcessorListProcessor> CODEC = StructureProcessorType.LIST_CODEC
		.xmap(ProcessorListProcessor::new, ProcessorListProcessor::processors)
		.fieldOf("processors");
	
	/** minecraft:worldgen/structure_processor / structurebuddy:processor_list **/
	public static final ResourceKey<StructureProcessorType<?>> KEY = ResourceKey.create(Registries.STRUCTURE_PROCESSOR, StructureBuddy.id("processor_list"));
	/** holder **/
	public static final DeferredHolder<StructureProcessorType<?>,StructureProcessorType<ProcessorListProcessor>> HOLDER = DeferredHolder.create(KEY);
		
	private final Holder<StructureProcessorList> processors;
	
	/**
	 * It's a constructor!
	 * @param processors Holder referencing a processor_list file
	 */
	public ProcessorListProcessor(Holder<StructureProcessorList> processors)
	{
		this.processors = processors;
	}

	/// {@return Holder referencing list of sub-processors to run}
	public Holder<StructureProcessorList> processors()
	{
		return this.processors;
	}
	
	@Override
	protected StructureProcessorType<?> getType()
	{
		return HOLDER.get();
	}

	@Override
	public @Nullable StructureBlockInfo process(
		LevelReader level,
		BlockPos targetPosition,
		BlockPos referencePos,
		StructureBlockInfo originalBlockInfo,
		StructureBlockInfo processedBlockInfo,
		StructurePlaceSettings settings,
		@Nullable StructureTemplate template)
	{
		@Nullable StructureBlockInfo modified = processedBlockInfo;
		for (var processor : this.processors.value().list())
		{
			modified = processor.process(level, targetPosition, referencePos, originalBlockInfo, modified, settings, template);
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
		StructurePlaceSettings settings)
	{
		for (var processor : this.processors.value().list())
		{
			processedBlockInfoList = processor.finalizeProcessing(level, position, referencePos, originalBlockInfoList, processedBlockInfoList, settings);
		}
		return processedBlockInfoList;
	}

	@SuppressWarnings({ "null", "unused" }) // neoforge api spec is currently incorrect, processEntity can and does return null
	@Override
	public @Nullable StructureEntityInfo processEntity(
		LevelReader world,
		BlockPos seedPos,
		StructureEntityInfo rawEntityInfo,
		StructureEntityInfo entityInfo,
		StructurePlaceSettings placementSettings,
		@Nullable StructureTemplate template)
	{
		@Nullable StructureEntityInfo modifiedInfo = entityInfo;
		for (var processor : this.processors.value().list())
		{
			modifiedInfo = processor.processEntity(world, seedPos, rawEntityInfo, modifiedInfo, placementSettings, template);
			if (modifiedInfo == null)
				break;
		}
		return modifiedInfo;
	}
}
