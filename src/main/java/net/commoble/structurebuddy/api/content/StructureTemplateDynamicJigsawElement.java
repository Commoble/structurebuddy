package net.commoble.structurebuddy.api.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.JigsawConnectionToParent;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.SelectableJigsawConnectionToParent;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.JigsawBlockInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DynamicJigsawElement (and PieceFiller) which places a structure template (i.e. structure nbt file) analogous to SinglePoolElement in vanilla jigsaw structures
 * @param location ResourceLocation of structure template
 * @param processors Optional StructureProcessorList to apply to structure template while placing blocks into world
 * @param overrideLiquidSettings Optional LiquidSettings to override for this jigsaw piece instead of using the root Structure liquid settings
 */
public record StructureTemplateDynamicJigsawElement(
	ResourceLocation location,
	Optional<Holder<StructureProcessorList>> processors,
	Optional<LiquidSettings> overrideLiquidSettings
	) implements DynamicJigsawElement, PieceFiller
{
	/** structurebuddy:dynamic_jigsaw_element_type / structurebuddy:structure_template **/
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> ELEMENT_KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("structure_template"));
	/** holder **/
	public static final DeferredHolder<MapCodec<? extends DynamicJigsawElement>, MapCodec<StructureTemplateDynamicJigsawElement>> ELEMENT_HOLDER = DeferredHolder.create(ELEMENT_KEY);
	/** structurebuddy:piece_filler_type / structurebuddy:structure_template **/
	public static final ResourceKey<MapCodec<? extends PieceFiller>> PIECE_FILLER_KEY = ResourceKey.create(StructureBuddyRegistries.PIECE_FILLER_TYPE, StructureBuddy.id("structure_template"));
	/** holder **/
	public static final DeferredHolder<MapCodec<? extends PieceFiller>, MapCodec<StructureTemplateDynamicJigsawElement>> PIECE_FILLER_HOLDER = DeferredHolder.create(PIECE_FILLER_KEY);
	
	/**
	 * e.g. 
	<pre>
	{
		"type": "structurebuddy:structure_template",
		"location": "yourmod:some_structure_template", // id of structure nbt file
		"processors": "yourmod:some_processor_list", // id of processor list file; optional, defaults to no processors
		"override_liquid_settings": true // optional, if not true or false then defaults to liquid settings from structure json
	}
	</pre>
	 */
	public static final MapCodec<StructureTemplateDynamicJigsawElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			ResourceLocation.CODEC.fieldOf("location").forGetter(StructureTemplateDynamicJigsawElement::location),
			StructureProcessorType.LIST_CODEC.optionalFieldOf("processors").forGetter(StructureTemplateDynamicJigsawElement::processors),
			LiquidSettings.CODEC.optionalFieldOf("override_liquid_settings").forGetter(StructureTemplateDynamicJigsawElement::overrideLiquidSettings)
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
			DynamicJigsawResult.addConnectionsFromTemplateJigsaw(jigsaw, shuffledConnectionsToParent, connectionsToChildren);
		}
		shuffledConnectionsToParent.sort(Comparator.comparingInt(SelectableJigsawConnectionToParent::selectionPriority).reversed());
		List<JigsawConnectionToParent> selectedConnectionsToParent = new ArrayList<>(shuffledConnectionsToParent.size());
		for (var selectable : shuffledConnectionsToParent)
		{
			selectedConnectionsToParent.add(selectable.connection());
		}
		return DynamicJigsawResult.withParentsAndChildren(this, localBoundingBox, selectedConnectionsToParent, connectionsToChildren);
	}


	@Override
	public void fill(DynamicJigsawFillContext context)
	{
		StructureTemplate template = context.structureTemplateManager().getOrCreate(this.location);
        StructurePlaceSettings placeSettings = new StructurePlaceSettings();
        placeSettings.setBoundingBox(context.chunkBoundingBox());
        placeSettings.setRotation(context.rotation());
        placeSettings.setKnownShape(true);
        placeSettings.setIgnoreEntities(false);
        placeSettings.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        placeSettings.setFinalizeEntities(true);
        placeSettings.setLiquidSettings(this.overrideLiquidSettings().orElse(context.liquidSettings()));
        // we don't need to retain jigsaws because vanilla jigsaw blocks can't generate our stuff anyway
        placeSettings.addProcessor(JigsawReplacementProcessor.INSTANCE);
        this.processors.ifPresent(processors -> processors.value().list().forEach(placeSettings::addProcessor));
        BoundingBox pieceBounds = context.pieceBoundingBox();
        BlockPos piecePos = new BlockPos(pieceBounds.minX(), pieceBounds.minY(), pieceBounds.minZ());
        // again, the rotation causes the placement box to be not quite where we want it to be
        // so, get where the template intends to place the box
        // compare that to where we thought it would place it, then add the difference as an offset
        BoundingBox whereTemplateWantsToPlace = template.getBoundingBox(placeSettings, piecePos);
        BlockPos posWhereTemplateWantsToPlace = new BlockPos(whereTemplateWantsToPlace.minX(), whereTemplateWantsToPlace.minY(), whereTemplateWantsToPlace.minZ());
        BlockPos offset = piecePos.subtract(posWhereTemplateWantsToPlace);
        BlockPos correctedTemplatePos = piecePos.offset(offset);
        // 18 is a block flag, same as SinglePoolElement
        template.placeInWorld(context.level(), correctedTemplatePos, context.startPieceFloorCenter(), placeSettings, context.random(), 18);
        // we don't support data blocks
	}
}
