package net.commoble.structurebuddy.api.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawBakeContext;
import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.JigsawConnectionToParent;
import net.commoble.structurebuddy.api.JigsawOverrides;
import net.commoble.structurebuddy.api.SelectableJigsawConnectionToParent;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.JigsawBlockInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DynamicJigsawElement which places a structure template (i.e. structure nbt file) analogous to SinglePoolElement in vanilla jigsaw structures
 * @param location Identifier of structure template
 * @param processors Optional StructureProcessorList to apply to structure template while placing blocks into world
 * @param overrideLiquidSettings Optional LiquidSettings to override for this jigsaw piece instead of using the root Structure liquid settings
 * @param jigsawOverrides Map of jigsaw name to JigsawOverrides to apply to jigsaws with that name
 */
public record StructureTemplateDynamicJigsawElement(
	Identifier location,
	Optional<Holder<StructureProcessorList>> processors,
	Optional<LiquidSettings> overrideLiquidSettings,
	Map<Identifier,JigsawOverrides> jigsawOverrides
	) implements DynamicJigsawElement
{
	/** structurebuddy:dynamic_jigsaw_element_type / structurebuddy:structure_template **/
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> ELEMENT_KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("structure_template"));
	/** holder **/
	public static final DeferredHolder<MapCodec<? extends DynamicJigsawElement>, MapCodec<StructureTemplateDynamicJigsawElement>> HOLDER = DeferredHolder.create(ELEMENT_KEY);

	/**
	 * e.g. 
	<pre>
	{
		"type": "structurebuddy:structure_template",
		"location": "yourmod:some_structure_template", // id of structure nbt file
		"processors": "yourmod:some_processor_list", // id of processor list file; optional, defaults to no processors
		"override_liquid_settings": true, // optional, if not true or false then defaults to liquid settings from structure json
		"jigsaw_overrides": { // optional map
			"yourmod:bottom": { // name of jigsaw, can override jigsaw parameters for jigsaws with this name
				"name": "yourmod:stairs_bottom",
				"target_pool": "yourmod:stairs_going_down",
				"target_name": "yourmod:stairs_top"
			}
		},
	}
	</pre>
	 */
	public static final MapCodec<StructureTemplateDynamicJigsawElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Identifier.CODEC.fieldOf("location").forGetter(StructureTemplateDynamicJigsawElement::location),
			StructureProcessorType.LIST_CODEC.optionalFieldOf("processors").forGetter(StructureTemplateDynamicJigsawElement::processors),
			LiquidSettings.CODEC.optionalFieldOf("override_liquid_sttings").forGetter(StructureTemplateDynamicJigsawElement::overrideLiquidSettings),
			JigsawOverrides.BY_JIGSAW_NAME_CODEC.optionalFieldOf("jigsaw_overrides", Map.of()).forGetter(StructureTemplateDynamicJigsawElement::jigsawOverrides)
		).apply(builder, StructureTemplateDynamicJigsawElement::new));

	@Override
	public MapCodec<? extends StructureTemplateDynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		GenerationContext generationContext = context.generationContext();
		StructureTemplate template = generationContext.structureTemplateManager().getOrCreate(location);
		Rotation rotation = context.rotation();
		BoundingBox localBoundingBox = template.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), BlockPos.ZERO);
		List<JigsawBlockInfo> jigsaws = template.getJigsaws(BlockPos.ZERO, rotation); // arraylist
		// shuffle jigsaws
		List<SelectableJigsawConnectionToParent> shuffledConnectionsToParent = new ArrayList<>(jigsaws.size());
		List<JigsawConnectionToChild> connectionsToChildren = new ArrayList<>(jigsaws.size());
		while (!jigsaws.isEmpty())
		{
			JigsawBlockInfo jigsaw = jigsaws.remove(generationContext.random().nextInt(jigsaws.size()));
			DynamicJigsawResult.addConnectionsFromTemplateJigsaw(jigsaw, shuffledConnectionsToParent, connectionsToChildren, this.jigsawOverrides);
		}
		shuffledConnectionsToParent.sort(Comparator.comparingInt(SelectableJigsawConnectionToParent::selectionPriority).reversed());
		List<JigsawConnectionToParent> selectedConnectionsToParent = new ArrayList<>(shuffledConnectionsToParent.size());
		for (var selectable : shuffledConnectionsToParent)
		{
			selectedConnectionsToParent.add(selectable.connection());
		}
		return DynamicJigsawResult.withParentsAndChildren(new StructureTemplatePieceFiller(this.location, this.processors, this.overrideLiquidSettings), localBoundingBox, selectedConnectionsToParent, connectionsToChildren);
	}
}
