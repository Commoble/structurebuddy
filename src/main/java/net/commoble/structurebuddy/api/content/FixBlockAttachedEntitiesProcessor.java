package net.commoble.structurebuddy.api.content;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.StructureBuddy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * StructureProcessor for processing BlockAttachedEntities (leash knots, item frames, paintings)
 * Fixes entity nbt data so that they are correctly positioned and rotated and no errors are raised when structures are generated
 */
public class FixBlockAttachedEntitiesProcessor extends StructureProcessor
{
	/// minecraft:worldgen/structure_processor / structurebuddy:fix_block_attached_entities
	public static final ResourceKey<StructureProcessorType<?>> KEY = ResourceKey.create(Registries.STRUCTURE_PROCESSOR, StructureBuddy.id("fix_block_attached_entities"));
	/// holder
	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<ItemFrameLootProcessor>> HOLDER = DeferredHolder.create(KEY);
	
	/// ```json
	/// {
	/// 	"processor_type": "structurebuddy:fix_block_attached_entities",
	/// 	"entity_types": "#c:item_frames",
	/// 	"facing_type": "item_frame" // optional, can be "item_frame" or "painting" or absent
	/// }
	/// ```
	public static final MapCodec<FixBlockAttachedEntitiesProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			HolderSetCodec.create(Registries.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.holderByNameCodec(), false).fieldOf("entity_types").forGetter(FixBlockAttachedEntitiesProcessor::entityTypes),
			FacingType.CODEC.optionalFieldOf("facingType").forGetter(FixBlockAttachedEntitiesProcessor::facingType)
		).apply(instance, FixBlockAttachedEntitiesProcessor::new));
	
	private final HolderSet<EntityType<?>> entityTypes;
	private final Optional<FacingType> facingType;
	
	/**
	 * It's a constructor!
	 * @param entityTypes HolderSet of entitytypes to apply fixes to
	 * @param facingType FacingType expected of entity types to apply fixes to
	 */
	public FixBlockAttachedEntitiesProcessor(HolderSet<EntityType<?>> entityTypes, Optional<FacingType> facingType)
	{
		this.entityTypes = entityTypes;
		this.facingType = facingType;
	}
	
	/// {@return HolderSet of entitytypes to apply this processor to}
	public HolderSet<EntityType<?>> entityTypes()
	{
		return this.entityTypes;
	}
	
	/// {@return FacingType for serializing facing, if any}
	public Optional<FacingType> facingType()
	{
		return this.facingType;
	}

	@Override
	protected StructureProcessorType<?> getType()
	{
		return HOLDER.get();
	}
	
	@Override
	public StructureEntityInfo processEntity(LevelReader levelReader, BlockPos seedPos, StructureEntityInfo rawEntityInfo, StructureEntityInfo entityInfo, StructurePlaceSettings placementSettings, StructureTemplate template)
	{
		StructureEntityInfo currentInfo = super.processEntity(levelReader, seedPos, rawEntityInfo, entityInfo, placementSettings, template);
		
		CompoundTag entityNBT = currentInfo.nbt;
		if (entityNBT
			.getString("id")
			.flatMap(stringId -> BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(stringId)))
			.map(this.entityTypes::contains).orElse(false))
		{
			// there's two things we need to fix, the blockpos and the facing
			// BlockAttachedEntity defines a "block_pos" field which must be set to the structure info's pos
			entityNBT.store("block_pos", BlockPos.CODEC, currentInfo.blockPos);
			
			// some hanging entities have a facing property (paintings and item frames)
			this.facingType.ifPresent(type -> {
				entityNBT.read(type.facingProperty, type.codec).ifPresent(facing -> {
					entityNBT.store(type.facingProperty, type.codec, placementSettings.getRotation().rotate(facing));
				});
			});
		}
		
		return currentInfo;
	}
	
	/// Enum defining how an entitytype serializes its facing.
	/// 
	/// Currently only vanilla entitytypes are supported, mods should design their entity types
	/// such that structures can place them without needing additional help.
	@SuppressWarnings("deprecation")
	public static enum FacingType implements StringRepresentable
	{
		/// Facing seriallizer for {@link ItemFrame}s
		ITEM_FRAME("Facing", Direction.LEGACY_ID_CODEC),
		/// Facing serializer for {@link Painting}s.
		PAINTING("facing", Direction.LEGACY_ID_CODEC_2D);
		
		/// ```json
		/// "some_field": "item_frame"
		/// ```
		public static final Codec<FacingType> CODEC = StringRepresentable.fromEnum(FacingType::values);
		
		private final String facingProperty;
		private final Codec<Direction> codec;
		
		private FacingType(String facingProperty, Codec<Direction> codec)
		{
			this.facingProperty = facingProperty;
			this.codec = codec;
		}
		
		/// {@return String of the name of the field in entity data that facing is serialized to}
		public String facingProperty()
		{
			return this.facingProperty;
		}
		
		/// {@return Codec which the given entity type uses to read its facing property}
		public Codec<Direction> codec()
		{
			return this.codec;
		}

		@Override
		public String getSerializedName()
		{
			return this.name().toLowerCase(Locale.ROOT);
		}
		
	}
}