package net.commoble.structurebuddy.examplemod;

import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.examplemod.LooperDynamicJigsawElement.LooperPieceFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=StructureBuddy.MODID)
public class StructureBuddyExampleMod
{

	@SubscribeEvent
	public static void onRegistration(RegisterEvent event)
	{
		// not actually an example of how to generally register things
		// doing it this way because we can't have two Mod classes
		// register things in the manner in which you're accustomed
		if (event.getRegistryKey() == StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE)
		{
			event.register(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, reg -> reg.register(TestDynamicJigsawElement.KEY, TestDynamicJigsawElement.CODEC));
			event.register(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, reg -> reg.register(LooperDynamicJigsawElement.KEY, LooperDynamicJigsawElement.CODEC));
		}
		else if (event.getRegistryKey() == StructureBuddyRegistries.PIECE_FILLER_TYPE)
		{
			event.register(StructureBuddyRegistries.PIECE_FILLER_TYPE, reg -> reg.register(TestPieceFiller.KEY, TestPieceFiller.CODEC));
			event.register(StructureBuddyRegistries.PIECE_FILLER_TYPE, reg -> reg.register(LooperPieceFiller.KEY, LooperPieceFiller.CODEC));
		}
	}
}
