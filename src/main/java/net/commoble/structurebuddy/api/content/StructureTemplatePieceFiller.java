package net.commoble.structurebuddy.api.content;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.DynamicProcessor;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * PieceFiller which places a structure template (i.e. structure nbt file) analogous to SinglePoolElement in vanilla jigsaw structures
 * @param location Identifier of structure template
 * @param processors Optional List of processors to apply to structure template while placing blocks into world
 * @param overrideLiquidSettings Optional LiquidSettings to override for this jigsaw piece instead of using the root Structure liquid settings
 */
public record StructureTemplatePieceFiller(
	Identifier location,
	Optional<Holder<List<DynamicProcessor>>> processors,
	Optional<LiquidSettings> overrideLiquidSettings
	) implements PieceFiller
{

	/** structurebuddy:piece_filler_type / structurebuddy:structure_template **/
	public static final ResourceKey<MapCodec<? extends PieceFiller>> PIECE_FILLER_KEY = ResourceKey.create(StructureBuddyRegistries.PIECE_FILLER_TYPE, StructureBuddy.id("structure_template"));
	/** holder **/
	public static final DeferredHolder<MapCodec<? extends PieceFiller>, MapCodec<StructureTemplatePieceFiller>> PIECE_FILLER_HOLDER = DeferredHolder.create(PIECE_FILLER_KEY);

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
	public static final MapCodec<StructureTemplatePieceFiller> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Identifier.CODEC.fieldOf("location").forGetter(StructureTemplatePieceFiller::location),
			DynamicProcessor.LIST_HOLDER_CODEC.optionalFieldOf("processors").forGetter(StructureTemplatePieceFiller::processors),
			LiquidSettings.CODEC.optionalFieldOf("override_liquid_settings").forGetter(StructureTemplatePieceFiller::overrideLiquidSettings)
		).apply(builder, StructureTemplatePieceFiller::new));

	@Override
	public MapCodec<? extends PieceFiller> codec()
	{
		return CODEC;
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
        this.processors.ifPresent(processors -> placeSettings.addProcessor(new DynamicProcessorListProcessor(processors, context.jigsawData().toMap())));
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
