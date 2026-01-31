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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/// DynamicProcessor which runs each processor in another dynamic processor list
/// @param processors Holder referencing another dynamic processor list file to run
public record SubListDynamicProcessor(Holder<List<DynamicProcessor>> processors) implements DynamicProcessor
{
	/** structurebuddy:dynamic_structure_processor_type / structurebuddy:sublist */
	public static final ResourceKey<MapCodec<? extends DynamicProcessor>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_PROCESSOR_TYPE, StructureBuddy.id("subpool"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends DynamicProcessor>, MapCodec<SubListDynamicProcessor>> HOLDER = DeferredHolder.create(KEY);
	
	///```json
	///{
	///	"type": "structurebuddy:sublist",
	///	"processors": "modid:some_other_dynamic_processor_list"
	///} 
	///```
	public static final MapCodec<SubListDynamicProcessor> CODEC = DynamicProcessor.LIST_HOLDER_CODEC
		.xmap(SubListDynamicProcessor::new, SubListDynamicProcessor::processors)
		.fieldOf("values");
	
	@Override
	public MapCodec<? extends DynamicProcessor> codec()
	{
		return CODEC;
	}

	@Override
	public @Nullable StructureBlockInfo process(LevelReader level, BlockPos targetPosition, BlockPos referencePos, StructureBlockInfo originalBlockInfo,
		StructureBlockInfo processedBlockInfo, StructurePlaceSettings settings, @Nullable StructureTemplate template, JigsawPieceDataReader jigsawData)
	{
		@Nullable StructureBlockInfo modified = processedBlockInfo;
		for (var processor : this.processors.value())
		{
			modified = processor.process(level, targetPosition, referencePos, originalBlockInfo, processedBlockInfo, settings, template, jigsawData);
			if (modified == null)
				break;
		}
		return modified;
	}

	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos, List<StructureBlockInfo> originalBlockInfoList,
		List<StructureBlockInfo> processedBlockInfoList, StructurePlaceSettings settings, JigsawPieceDataReader jigsawData)
	{
		List<StructureBlockInfo> modified = processedBlockInfoList;
		for (var processor : this.processors.value())
		{
			modified = processor.finalizeProcessing(level, position, referencePos, originalBlockInfoList, processedBlockInfoList, settings, jigsawData);
		}
		return modified;
	}

	@Override
	public @Nullable StructureEntityInfo processEntity(LevelReader world, BlockPos seedPos, StructureEntityInfo rawEntityInfo, StructureEntityInfo entityInfo,
		StructurePlaceSettings placementSettings, @Nullable StructureTemplate template, JigsawPieceDataReader jigsawData)
	{
		@Nullable StructureEntityInfo modified = entityInfo;
		for (var processor : this.processors.value())
		{
			modified = processor.processEntity(world, seedPos, rawEntityInfo, entityInfo, placementSettings, template, jigsawData);
			if (modified == null)
				break;
		}
		return modified;
	}

	
}
