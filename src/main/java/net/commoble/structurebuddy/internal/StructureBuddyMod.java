package net.commoble.structurebuddy.internal;

import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawPool;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructure;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructurePiece;
import net.commoble.structurebuddy.api.content.EmptyDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.EmptyPieceFiller;
import net.commoble.structurebuddy.api.content.FeatureDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.StructureTemplateDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.SubPoolDynamicJigsawElement;
import net.commoble.structurebuddy.api.content.FeatureDynamicJigsawElement.FeaturePieceFiller;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
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
		DeferredRegister<StructureType<?>> structureTypes = defreg(Registries.STRUCTURE_TYPE);
		DeferredRegister<StructurePieceType> structurePieceTypes = defreg(Registries.STRUCTURE_PIECE);
		DeferredRegister<MapCodec<? extends DynamicJigsawElement>> dynamicJigsawElementTypes = newRegistry(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE);
		DeferredRegister<MapCodec<? extends PieceFiller>> pieceFillerTypes = newRegistry(StructureBuddyRegistries.PIECE_FILLER_TYPE);

		structureTypes.<StructureType<DynamicJigsawStructure>>register(
			"dynamic_jigsaw",
			() -> () -> DynamicJigsawStructure.CODEC);

		structurePieceTypes.register("dynamic_jigsaw",
			() -> DynamicJigsawStructurePiece::new);
		
		dynamicJigsawElementTypes.register("empty", () -> EmptyDynamicJigsawElement.CODEC);
		dynamicJigsawElementTypes.register("structure_template", () -> StructureTemplateDynamicJigsawElement.CODEC);
		dynamicJigsawElementTypes.register("subpool", () -> SubPoolDynamicJigsawElement.CODEC);
		dynamicJigsawElementTypes.register("feature", () -> FeatureDynamicJigsawElement.CODEC);
		
		pieceFillerTypes.register("empty", () -> EmptyPieceFiller.CODEC);
		pieceFillerTypes.register("structure_template", () -> StructureTemplateDynamicJigsawElement.CODEC);
		pieceFillerTypes.register("feature", () -> FeaturePieceFiller.CODEC);
		
		modBus.addListener(this::onRegisterDatapackRegistries);
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
		event.dataPackRegistry(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, DynamicJigsawPool.DIRECT_CODEC);
	}
	
}
