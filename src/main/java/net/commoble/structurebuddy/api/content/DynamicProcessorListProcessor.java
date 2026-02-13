package net.commoble.structurebuddy.api.content;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicProcessor;
import net.commoble.structurebuddy.api.JigsawDataType;
import net.commoble.structurebuddy.api.JigsawPieceDataReader;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/// StructureProcessor impl which delegates to a DynamicProcessor list
public class DynamicProcessorListProcessor extends StructureProcessor implements JigsawPieceDataReader
{
	/// ```json
	/// {
	/// 	"processor_type": "structurebuddy": "dynamic_processor_list",
	/// 	"processors": [
	/// 		{
	/// 			// dynamic processor object
	/// 		}
	/// 	],
	/// 	"jigsaw_data": {
	/// 		// jigsaw data values keyed by JigsawDataType ids
	/// 	}
	/// }
	/// ```
	public static final MapCodec<DynamicProcessorListProcessor> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			DynamicProcessor.LIST_HOLDER_CODEC.fieldOf("processors").forGetter(DynamicProcessorListProcessor::processors),
			JigsawDataType.MAP_CODEC.fieldOf("jigsaw_data").forGetter(DynamicProcessorListProcessor::jigsawData)
		).apply(builder, DynamicProcessorListProcessor::new));
	
	/** minecraft:worldgen/structure_processor / structurebuddy:dynamic_processor_list **/
	public static final ResourceKey<StructureProcessorType<?>> KEY = ResourceKey.create(Registries.STRUCTURE_PROCESSOR, StructureBuddy.id("dynamic_processor_list"));
	/** holder **/
	public static final DeferredHolder<StructureProcessorType<?>,StructureProcessorType<DynamicProcessorListProcessor>> HOLDER = DeferredHolder.create(KEY);
		
	private final Holder<List<DynamicProcessor>> processors;
	private final Map<JigsawDataType<?>, Object> jigsawData;
	
	/**
	 * It's a constructor!
	 * @param processors Holder referencing a dynamicprocessor list; can be a Reference to a dynamic_processor_list file
	 * @param jigsawData Map of jigsaw data to pass to each processor
	 */
	public DynamicProcessorListProcessor(Holder<List<DynamicProcessor>> processors, Map<JigsawDataType<?>, Object> jigsawData)
	{
		this.processors = processors;
		this.jigsawData = jigsawData;
	}

	/// {@return Holder referencing list of sub-processors to run}
	public Holder<List<DynamicProcessor>> processors()
	{
		return this.processors;
	}
	
	/// {@return Map of jigsaw data; objects in map must be considered deeply immutable, like DataComponents}
	public Map<JigsawDataType<?>, Object> jigsawData()
	{
		return this.jigsawData;
	}
	
	@Override
	protected StructureProcessorType<?> getType()
	{
		return HOLDER.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getData(JigsawDataType<T> key)
	{
		return (T) this.jigsawData.get(key);
	}

	@Override
	public Map<JigsawDataType<?>, Object> toMap()
	{
		return Map.copyOf(this.jigsawData);
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
		for (DynamicProcessor processor : this.processors.value())
		{
			modified = processor.process(level, targetPosition, referencePos, originalBlockInfo, modified, settings, template, this);
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
		for (DynamicProcessor processor : this.processors.value())
		{
			processedBlockInfoList = processor.finalizeProcessing(level, position, referencePos, originalBlockInfoList, processedBlockInfoList, settings, this);
		}
		return processedBlockInfoList;
	}

	@SuppressWarnings("null") // neoforge api spec is currently incorrect, processEntity can and does return null
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
		for (DynamicProcessor processor : this.processors.value())
		{
			modifiedInfo = processor.processEntity(world, seedPos, rawEntityInfo, modifiedInfo, placementSettings, template, this);
			if (modifiedInfo == null)
				break;
		}
		return modifiedInfo;
	}
}
