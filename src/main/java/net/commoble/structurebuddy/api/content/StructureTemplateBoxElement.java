package net.commoble.structurebuddy.api.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.function.Consumers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.BoxBakeContext;
import net.commoble.structurebuddy.api.BoxElement;
import net.commoble.structurebuddy.api.BoxResult;
import net.commoble.structurebuddy.api.BoxSnap;
import net.commoble.structurebuddy.api.BoxSnap.FaceBoxSnap;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.JigsawConnectionToParent;
import net.commoble.structurebuddy.api.JigsawOverrides;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.SelectableJigsawConnectionToParent;
import net.commoble.structurebuddy.api.SnapResult;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.api.util.BoxBuddy;
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
 * BoxElement which bakes some structure template (.nbt structure file) into a box
 * @param location Identifier of structure template
 * @param processors Optional StructureProcessorList to apply to structure template while placing blocks into world
 * @param overrideLiquidSettings Optional LiquidSettings to override for this jigsaw piece instead of using the root Structure liquid settings
 * @param jigsawOverrides Map of jigsaw name to JigsawOverrides to apply to jigsaws with that name
 * @param snap BoxSnap indicating directions of available surfaces to snap to if possible. Template position will be randomized on non-snapping axes.
 */
public record StructureTemplateBoxElement(
	Identifier location,
	Optional<Holder<StructureProcessorList>> processors,
	Optional<LiquidSettings> overrideLiquidSettings,
	Map<Identifier, JigsawOverrides> jigsawOverrides,
	BoxSnap snap) implements BoxElement
{

	/** structurebuddy:dynamic_jigsaw_element_type / structurebuddy:structure_template **/
	public static final ResourceKey<MapCodec<? extends BoxElement>> KEY = ResourceKey.create(StructureBuddyRegistries.BOX_ELEMENT_TYPE, StructureBuddy.id("structure_template"));
	/** holder **/
	public static final DeferredHolder<MapCodec<? extends BoxElement>, MapCodec<StructureTemplateBoxElement>> HOLDER = DeferredHolder.create(KEY);

	/**
	 * e.g. 
	<pre>
	{
		"type": "structurebuddy:structure_template",
		"location": "yourmod:some_structure_template", // id of structure nbt file
		"processors": "yourmod:some_processor_list", // id of processor list file; optional, defaults to no processors
		"override_liquid_settings": true // optional, if not true or false then defaults to liquid settings from structure json
		"jigsaw_overrides": { // optional map
			"yourmod:bottom": { // name of jigsaw, can override jigsaw parameters for jigsaws with this name
				"name": "yourmod:stairs_bottom",
				"target_pool": "yourmod:stairs_going_down",
				"target_name": "yourmod:stairs_top"
			}
		},
		"snap": "floor" // optional, defaults to floor
	}
	</pre>
	 */
	public static final MapCodec<StructureTemplateBoxElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Identifier.CODEC.fieldOf("location").forGetter(StructureTemplateBoxElement::location),
			StructureProcessorType.LIST_CODEC.optionalFieldOf("processors").forGetter(StructureTemplateBoxElement::processors),
			LiquidSettings.CODEC.optionalFieldOf("override_liquid_settings").forGetter(StructureTemplateBoxElement::overrideLiquidSettings),
			JigsawOverrides.BY_JIGSAW_NAME_CODEC.optionalFieldOf("jigsaw_overrides", Map.of()).forGetter(StructureTemplateBoxElement::jigsawOverrides),
			BoxSnap.CODEC.optionalFieldOf("snap", FaceBoxSnap.FLOOR).forGetter(StructureTemplateBoxElement::snap)
		).apply(builder, StructureTemplateBoxElement::new));

	@Override
	public MapCodec<? extends BoxElement> codec()
	{
		return CODEC;
	}

	@Override
	public BoxResult bake(BoxBakeContext context)
	{
		GenerationContext generationContext = context.generationContext();
		StructureTemplate template = generationContext.structureTemplateManager().getOrCreate(location);
		Rotation rotation = context.rotation();
		BoundingBox localBoundingBox = template.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), BlockPos.ZERO);
		BoundingBox parentBox = context.box();
		if (BoxBuddy.isLargerThan(localBoundingBox, parentBox))
		{
			return BoxResult.invalid();
		}
		
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
		SnapResult snapResult = this.snap.getSnap(selectedConnectionsToParent, context.boundingSurfaces(), context.generationContext().random());
		if (snapResult == null)
			return BoxResult.invalid();
		BoundingBox finalBox = snapResult.snap(localBoundingBox, parentBox, context.generationContext().random());
		BlockPos jigsawOffset = BoxBuddy.minCorner(finalBox).subtract(BoxBuddy.minCorner(localBoundingBox));
		List<JigsawConnectionToParent> finalConnectionsToParent = new ArrayList<>();
		List<JigsawConnectionToChild> finalConnectionsToChild = new ArrayList<>();
		for (JigsawConnectionToParent connection : selectedConnectionsToParent)
		{
			finalConnectionsToParent.add(new JigsawConnectionToParent(
				connection.pos().offset(jigsawOffset),
				connection.orientation(),
				connection.name(),
				connection.placementPriority()));
		}
		for (JigsawConnectionToChild connection : connectionsToChildren)
		{
			finalConnectionsToChild.add(new JigsawConnectionToChild(
				connection.pos().offset(jigsawOffset),
				connection.orientation(),
				connection.jointType(),
				connection.pool(),
				connection.target()));
		}
		
		PieceFiller pieceFiller = new StructureTemplatePieceFiller(this.location, this.processors, this.overrideLiquidSettings);
		return new BoxResult(() -> pieceFiller, finalBox, finalConnectionsToParent, finalConnectionsToChild, Consumers.nop());
	}
}
