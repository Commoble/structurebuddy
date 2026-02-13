package net.commoble.structurebuddy.api.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.tags.StructureBuddyCommonEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * StructureProcessor for processing item frames.
 * If processed entity is an item frame, a loot table is rolled and the result added to the frame.
 * Applies to any entity in the c:item_frames entity_type tag.
 */
public class ItemFrameLootProcessor extends StructureProcessor
{
	/// minecraft:worldgen/structure_processor / structurebuddy:item_frame_loot
	public static final ResourceKey<StructureProcessorType<?>> KEY = ResourceKey.create(Registries.STRUCTURE_PROCESSOR, StructureBuddy.id("item_frame_loot"));
	/// holder
	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<ItemFrameLootProcessor>> HOLDER = DeferredHolder.create(KEY);
	
	/// ```json
	/// {
	/// 	"processor_type": "structurebuddy:item_frame_loot",
	/// 	"loot_table": "modid:loot_table_id"
	/// }
	/// ```
	public static final MapCodec<ItemFrameLootProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(ItemFrameLootProcessor::getLootTable)
		).apply(instance, ItemFrameLootProcessor::new));

	private final ResourceKey<LootTable> lootTable;
	
	/// {@return ResourceKey of loot table to roll for item frame}
	public ResourceKey<LootTable> getLootTable() { return this.lootTable; }
	
	/**
	 * It's a constructor!
	 * @param lootTable ResourceKey of loot table to roll for item frame
	 */
	public ItemFrameLootProcessor(ResourceKey<LootTable> lootTable)
	{
		this.lootTable = lootTable;
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
		
		if (levelReader instanceof ServerLevelAccessor serverLevelAccessor)
		{
			CompoundTag entityNBT = currentInfo.nbt;
			entityNBT.getString("id").ifPresent(stringId -> {
				Identifier id = Identifier.parse(stringId);
				BuiltInRegistries.ENTITY_TYPE.get(id).ifPresent(holder -> {
					if (holder.is(StructureBuddyCommonEntityTypeTags.ITEM_FRAMES))
					{
						// generate and set itemstack
						ItemStack stack = this.generateItemStack(serverLevelAccessor.getLevel(), currentInfo.blockPos);
						currentInfo.nbt.store("Item", ItemStack.CODEC, stack);
					}
				});
			});
		}
		
		return currentInfo;
	}
	
	private ItemStack generateItemStack(ServerLevel level, BlockPos pos)
	{
		@Nullable MinecraftServer server = level.getServer();
		if (server == null)
			return ItemStack.EMPTY;
		
		// Loot tables aren't threadsafe.
		// If the root loot table doesn't have a random sequence, the ServerLevel's random is used, which is not threadsafe.
		// If the root table DOES have a random sequence, the loot table's random is computeIfAbsent-ed, which is not threadsafe.
		// Only way to make them threadsafe seems to be to sneak our own random into the context builder,
		// which causes the other two things to not happen.
		long hashedSeed = level.getSeed() + level.dimension().identifier().hashCode() + pos.hashCode();
		RandomSource random = new XoroshiroRandomSource(hashedSeed);
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)) // positional context
			.create(LootContextParamSets.CHEST);	// chest set requires positional context, has no other mandatory parameters
		LootTable table = server
			.reloadableRegistries()
			.getLootTable(this.lootTable);
		var contextBuilder = new LootContext.Builder(params)
			.withOptionalRandomSource(random);
		LootContext context = contextBuilder.create(Optional.empty()); // we already set the random so it will ignore the id param here
		List<ItemStack> stacks = new ArrayList<>();
		table.getRandomItems(context, stacks::add);
		return stacks.size() > 0
			? stacks.get(0)
			: ItemStack.EMPTY;
	}
}