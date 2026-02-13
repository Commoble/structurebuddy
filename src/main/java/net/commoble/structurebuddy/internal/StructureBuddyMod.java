package net.commoble.structurebuddy.internal;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.BoxElement;
import net.commoble.structurebuddy.api.BoxPool;
import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawPool;
import net.commoble.structurebuddy.api.DynamicProcessor;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.api.content.BlockStateProcessor;
import net.commoble.structurebuddy.api.content.BoxDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructure;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructurePiece;
import net.commoble.structurebuddy.api.content.DynamicProcessorListProcessor;
import net.commoble.structurebuddy.api.content.EmptyBoxElement;
import net.commoble.structurebuddy.api.content.EmptyDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.EmptyPieceFiller;
import net.commoble.structurebuddy.api.content.FeatureDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.FixBlockAttachedEntitiesProcessor;
import net.commoble.structurebuddy.api.content.FeatureDynamicJigsawElement.FeaturePieceFiller;
import net.commoble.structurebuddy.api.content.ItemFrameLootProcessor;
import net.commoble.structurebuddy.api.content.NopDynamicProcessor;
import net.commoble.structurebuddy.api.content.ProcessorListDynamicProcessor;
import net.commoble.structurebuddy.api.content.ProcessorListProcessor;
import net.commoble.structurebuddy.api.content.StructureTemplateBoxElement;
import net.commoble.structurebuddy.api.content.StructureTemplateDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.StructureTemplatePieceFiller;
import net.commoble.structurebuddy.api.content.SubListDynamicProcessor;
import net.minecraft.Optionull;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mod class for StructureBuddy
 */
@Mod(StructureBuddy.MODID)
@ApiStatus.Internal
public class StructureBuddyMod
{	
	/**
	 * Mod constructor for StructureBuddy
	 */
	@ApiStatus.Internal
	public StructureBuddyMod()
	{
		IEventBus modBus = ModList.get().getModContainerById(StructureBuddy.MODID).get().getEventBus();
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		
		// vanilla registries
		DeferredRegister<StructurePieceType> structurePieceTypes = defreg(Registries.STRUCTURE_PIECE);
		DeferredRegister<StructureProcessorType<?>> structureProcessorTypes = defreg(Registries.STRUCTURE_PROCESSOR);
		DeferredRegister<StructureType<?>> structureTypes = defreg(Registries.STRUCTURE_TYPE);
		
		// custom registries
		DeferredRegister<MapCodec<? extends BoxElement>> boxElementTypes = newRegistry(StructureBuddyRegistries.BOX_ELEMENT_TYPE);
		DeferredRegister<MapCodec<? extends DynamicJigsawElement>> dynamicJigsawElementTypes = newRegistry(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE);
		DeferredRegister<MapCodec<? extends DynamicProcessor>> dynamicProcessorTypes = newRegistry(StructureBuddyRegistries.DYNAMIC_PROCESSOR_TYPE);
		newRegistry(StructureBuddyRegistries.JIGSAW_DATA_TYPE);
		DeferredRegister<MapCodec<? extends PieceFiller>> pieceFillerTypes = newRegistry(StructureBuddyRegistries.PIECE_FILLER_TYPE);

		structurePieceTypes.register(DynamicJigsawStructurePiece.HOLDER.getId().getPath(),
			() -> DynamicJigsawStructurePiece::new);

		// for whatever reason
		// eclipse won't compile structureProcessorTypes.register(name, () -> () -> codec)
		// so, using a helper to shorten it from what we'd otherwise have to do here
		registerStructureProcessor(structureProcessorTypes, BlockStateProcessor.KEY, BlockStateProcessor.CODEC);
		registerStructureProcessor(structureProcessorTypes, DynamicProcessorListProcessor.KEY, DynamicProcessorListProcessor.CODEC);
		registerStructureProcessor(structureProcessorTypes, FixBlockAttachedEntitiesProcessor.KEY, FixBlockAttachedEntitiesProcessor.CODEC);
		registerStructureProcessor(structureProcessorTypes, ItemFrameLootProcessor.KEY, ItemFrameLootProcessor.CODEC);
		registerStructureProcessor(structureProcessorTypes, ProcessorListProcessor.KEY, ProcessorListProcessor.CODEC);
				
		structureTypes.<StructureType<DynamicJigsawStructure>>register(
			DynamicJigsawStructure.HOLDER.getId().getPath(),
			() -> () -> DynamicJigsawStructure.CODEC);
		
		boxElementTypes.register(EmptyBoxElement.HOLDER.getId().getPath(), () -> EmptyBoxElement.CODEC);
		boxElementTypes.register(StructureTemplateBoxElement.HOLDER.getId().getPath(), () -> StructureTemplateBoxElement.CODEC);
		
		dynamicJigsawElementTypes.register(EmptyDynamicJigsawElement.HOLDER.getId().getPath(), () -> EmptyDynamicJigsawElement.CODEC);
		dynamicJigsawElementTypes.register(BoxDynamicJigsawElement.HOLDER.getId().getPath(), () -> BoxDynamicJigsawElement.CODEC);
		dynamicJigsawElementTypes.register(FeatureDynamicJigsawElement.HOLDER.getId().getPath(), () -> FeatureDynamicJigsawElement.CODEC);
		dynamicJigsawElementTypes.register(StructureTemplateDynamicJigsawElement.HOLDER.getId().getPath(), () -> StructureTemplateDynamicJigsawElement.CODEC);
		
		dynamicProcessorTypes.register(NopDynamicProcessor.HOLDER.getId().getPath(), () -> NopDynamicProcessor.CODEC);
		dynamicProcessorTypes.register(SubListDynamicProcessor.HOLDER.getId().getPath(), () -> SubListDynamicProcessor.CODEC);
		dynamicProcessorTypes.register(ProcessorListDynamicProcessor.HOLDER.getId().getPath(), () -> ProcessorListDynamicProcessor.CODEC);
		
		pieceFillerTypes.register(EmptyPieceFiller.HOLDER.getId().getPath(), () -> EmptyPieceFiller.CODEC);
		pieceFillerTypes.register(FeaturePieceFiller.HOLDER.getId().getPath(), () -> FeaturePieceFiller.CODEC);
		pieceFillerTypes.register(StructureTemplatePieceFiller.PIECE_FILLER_HOLDER.getId().getPath(), () -> StructureTemplatePieceFiller.CODEC);
		
		modBus.addListener(this::onRegisterDatapackRegistries);
		
		forgeBus.addListener(this::onServerAboutToStart);
	}
	
	private static <T> DeferredRegister<T> defreg(ResourceKey<Registry<T>> registryKey)
	{
		return defreg(modid -> DeferredRegister.create(registryKey, modid));
	}
	
	private static <T, R extends DeferredRegister<T>> R defreg(Function<String, R> regFactory)
	{
		R register = regFactory.apply(StructureBuddy.MODID);
		register.register(ModList.get().getModContainerById(StructureBuddy.MODID).get().getEventBus());
		return register;
	}
	
	private static <T> DeferredRegister<T> newRegistry(ResourceKey<Registry<T>> registryKey)
	{
		DeferredRegister<T> register = DeferredRegister.create(registryKey, StructureBuddy.MODID);
		register.makeRegistry(builder->{});
		register.register(ModList.get().getModContainerById(StructureBuddy.MODID).get().getEventBus());
		return register;
	}
	
	private void onRegisterDatapackRegistries(DataPackRegistryEvent.NewRegistry event)
	{
		event.dataPackRegistry(StructureBuddyRegistries.BOX_ELEMENT, BoxElement.DIRECT_CODEC);
		event.dataPackRegistry(StructureBuddyRegistries.BOX_POOL, BoxPool.DIRECT_CODEC);
		event.dataPackRegistry(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT, DynamicJigsawElement.DIRECT_CODEC);
		event.dataPackRegistry(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, DynamicJigsawPool.DIRECT_CODEC);
		event.dataPackRegistry(StructureBuddyRegistries.DYNAMIC_PROCESSOR_LIST, DynamicProcessor.DIRECT_LIST_CODEC);
	}
	
	private void onServerAboutToStart(ServerAboutToStartEvent event)
	{
		RegistryAccess registries = event.getServer().registryAccess();
		// check pools for circular references
		Set<Holder<DynamicJigsawPool>> knownGoodJigsawPools = new HashSet<>();
		for (Holder<DynamicJigsawPool> holder : registries.lookupOrThrow(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL).asHolderIdMap())
		{
			validatePool(holder, knownGoodJigsawPools, new LinkedHashSet<>(), "structurebuddy/dynamic_jigsaw_pool", DynamicJigsawPool::delegates);
		}
		
		Set<Holder<BoxPool>> knownGoodBoxPools = new HashSet<>();
		for (Holder<BoxPool> holder : registries.lookupOrThrow(StructureBuddyRegistries.BOX_POOL).asHolderIdMap())
		{
			validatePool(holder, knownGoodBoxPools, new LinkedHashSet<>(), "structurebuddy/box_pool", BoxPool::delegates);
		}
	}
	
	private static <T> void validatePool(Holder<T> holder, Set<Holder<T>> knownGoodPools, SequencedSet<Holder<T>> ancestralPools, String poolTypeName, Function<T, WeightedList<HolderSet<T>>> delegateLookup)
	{
		if (knownGoodPools.contains(holder))
		{
			return;
		}
		
		if (ancestralPools.contains(holder))
		{
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(String.format("Circular reference found in registry %s:\n", poolTypeName));
			for (var parentHolder : ancestralPools)
			{
				stringBuilder.append(String.format(
					"* %s delegates to\n",
					Optionull.mapOrDefault(
						parentHolder.getKey(),
						key -> key.identifier().toString(),
						parentHolder.toString())));
			}
			stringBuilder.append(String.format(
				"* %s",
				Optionull.mapOrDefault(holder.getKey(), key -> key.identifier().toString(), holder.toString())));
			throw new IllegalStateException(stringBuilder.toString());
		}
		
		SequencedSet<Holder<T>> nextSet = new LinkedHashSet<>(ancestralPools);
		nextSet.add(holder);

		for (Weighted<HolderSet<T>> weightedHolderSet : delegateLookup.apply(holder.value()).unwrap())
		{
			for (Holder<T> childHolder : weightedHolderSet.value())
			{
				validatePool(childHolder, knownGoodPools, nextSet, poolTypeName, delegateLookup);
			}
		}
		
		knownGoodPools.add(holder);
	}
	
	private static <T extends StructureProcessor> Supplier<StructureProcessorType<T>> registerStructureProcessor(DeferredRegister<StructureProcessorType<?>> defreg, ResourceKey<StructureProcessorType<?>> key, MapCodec<T> codec)
	{
		return () -> () -> codec;
	}
}
