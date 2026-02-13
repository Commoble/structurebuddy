package net.commoble.structurebuddy.api.content;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.StructureBuddy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * StructureProcessor which applies blockstate properties from some given blockstate to blockstates in the structure template
 */
public class BlockStateProcessor extends StructureProcessor
{
	/// minecraft:worldgen/structure_processor / structurebuddy:blockstatet
	public static final ResourceKey<StructureProcessorType<?>> KEY = ResourceKey.create(Registries.STRUCTURE_PROCESSOR, StructureBuddy.id("blockstate"));
	/// holder
	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<BlockStateProcessor>> HOLDER = DeferredHolder.create(KEY);
	
	/// ```json
	/// {
	/// 	"type": "structurebuddy:blockstate",
	/// 	"input_predicate": {}, // optional RuleTest testing the current blockstate being processed
	/// 	"location_predicate": {}, // optional RuleTest testing the blockstate-in-world where the processed blockstate would be placed
	/// 	"position_predicate": {}, // optional PosRuleTest testing the position to place the block at
	///		"copy_from_blockstate": { // BlockState to copy properties from to the block being processed
	///			"Name": "candle",
	///			"Properties": {
	///				"lit": "true"					
	///			}
	///		},
	///		"properties": ["lit"] // list of property names to copy from the copy_from_blockstate
	/// ```
	public static final MapCodec<BlockStateProcessor> CODEC = RecordCodecBuilder.<BlockStateProcessor>mapCodec(builder -> builder.group(
			RuleTest.CODEC.optionalFieldOf("input_predicate", AlwaysTrueTest.INSTANCE).forGetter(BlockStateProcessor::inputPredicate),
			RuleTest.CODEC.optionalFieldOf("location_predicate", AlwaysTrueTest.INSTANCE).forGetter(BlockStateProcessor::locationPredicate),
			PosRuleTest.CODEC.optionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(BlockStateProcessor::positionPredicate),
			BlockState.CODEC.fieldOf("copy_from_blockstate").forGetter(BlockStateProcessor::copyFromBlockState),
			Codec.STRING.listOf().fieldOf("properties").forGetter(BlockStateProcessor::properties)
		).<BlockStateProcessor>apply(builder, BlockStateProcessor::new))
		.validate((BlockStateProcessor processor) -> {
			Block block = processor.copyFromBlockState().getBlock();
			var definition = block.getStateDefinition();
			for (String property : processor.properties())
			{
				if (definition.getProperty(property) == null)
				{
					return DataResult.error(() -> String.format("Block %s does not have property %s", block, property)); 
				}
			}
			return DataResult.<BlockStateProcessor>success(processor);
		});
		
	private final RuleTest inputPredicate;
	private final RuleTest locationPredicate;
	private final PosRuleTest positionPredicate;
	private final BlockState copyFromBlockState;
	private final List<String> properties;
	
	/// {@return RuleTest testing the current blockstate being processed}
	public RuleTest inputPredicate() { return this.inputPredicate; }
	/// {@return RuleTest testing the blockstate-in-world where the processed blockstate would be placed}
	public RuleTest locationPredicate() { return this.locationPredicate; }
	/// {@return PosRuleTest testing the position to place the block at}
	public PosRuleTest positionPredicate() { return this.positionPredicate; }
	/// {@return copyFromBlockState BlockState to copy properties from to the block being processed}
	public BlockState copyFromBlockState() { return this.copyFromBlockState; }
	/// {@return List of String property names to copy from the copyFromBlockState}
	public List<String> properties() { return this.properties; }
	
	/**
	 * It's a constructor!
	 * @param inputPredicate RuleTest testing the current blockstate being processed
	 * @param locationPredicate RuleTest testing the blockstate-in-world where the processed blockstate would be placed
	 * @param positionPredicate PosRuleTest testing the position to place the block at
	 * @param copyFromBlockState BlockState to copy properties from to the block being processed
	 * @param properties List of String property names to copy from the copyFromBlockState
	 */
	public BlockStateProcessor(
		RuleTest inputPredicate,
		RuleTest locationPredicate,
		PosRuleTest positionPredicate,
		BlockState copyFromBlockState,
		List<String> properties)
	{
		this.inputPredicate = inputPredicate;
		this.locationPredicate = locationPredicate;
		this.positionPredicate = positionPredicate;
		this.copyFromBlockState = copyFromBlockState;
		this.properties = properties;
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
        @SuppressWarnings("deprecation")
		RandomSource random = RandomSource.create(Mth.getSeed(processedBlockInfo.pos()));
		BlockState state = processedBlockInfo.state();
		if (this.inputPredicate.test(state, random))
		{
			BlockPos pos = processedBlockInfo.pos();
			BlockState worldState = level.getBlockState(pos);
			if (this.locationPredicate.test(worldState, random))
			{
				if (this.positionPredicate.test(originalBlockInfo.pos(), pos, referencePos, random))
				{
					BlockState finalState = state;
					var copyFromStates = this.copyFromBlockState.getBlock().getStateDefinition();
					for (String propertyName : this.properties)
					{
						Property<?> property = copyFromStates.getProperty(propertyName);
						if (property != null && finalState.hasProperty(property))
						{
							finalState = this.copyValueTo(property, finalState);
						}
					}
					processedBlockInfo = new StructureBlockInfo(processedBlockInfo.pos(), finalState, processedBlockInfo.nbt());
				}
			}
		}
		return processedBlockInfo;
	}
	
	private <T extends Comparable<T>> BlockState copyValueTo(Property<T> property, BlockState to)
	{
		T value = this.copyFromBlockState.getValue(property);
		return to.setValue(property, value);
	}
}
