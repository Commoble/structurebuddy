package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawPool;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DynamicJigsawElement which rolls a different jigsaw pool to produce results from.
 * Unlike normal use of jigsaw pools, the unused elements are not considered if the selected element's results are rejected.
 * May be best to use this with a pool of elements whose elements have the same size and connections.
 * @param value Holder of DynamicJigsawPool to roll and produce jigsaw results from
 */
public record SubPoolDynamicJigsawElement(Holder<DynamicJigsawPool> value) implements DynamicJigsawElement
{
	/** structurebuddy:dynamic_jigsaw_element_type / structurebuddy:subpool */
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("subpool"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends DynamicJigsawElement>, MapCodec<SubPoolDynamicJigsawElement>> HOLDER = DeferredHolder.create(KEY);
	/**
	 * e.g.
	 <pre>
	 {
	 	"type": "structurebuddy:subpool",
	 	"value": "yourmod:some_pool" // dynamic_jigsaw_pool file to roll
	 }
	 </pre>
	 */
	public static final MapCodec<SubPoolDynamicJigsawElement> CODEC = DynamicJigsawPool.CODEC.fieldOf("value")
		.xmap(SubPoolDynamicJigsawElement::new, SubPoolDynamicJigsawElement::value);
	
	@Override
	public MapCodec<? extends DynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		return this.value
			.value()
			.elements()
			.getRandom(context.generationContext().random())
			.map(element -> element.bake(context))
			.orElse(DynamicJigsawResult.EMPTY);
	}

}
