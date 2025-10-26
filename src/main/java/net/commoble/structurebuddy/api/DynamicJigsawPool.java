package net.commoble.structurebuddy.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.content.DynamicJigsawStructure;
import net.commoble.structurebuddy.api.content.StructureTemplateDynamicJigsawElement;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

/**
 * DynamicJigsawPools are defined in JSON files in the structurebuddy:dynamic_jigsaw_pool datapack registry.
 * (i.e. data/yourmodid/structurebuddy/dynamic_jigsaw_pool/yourfile.json)
 * These are used with the {@link DynamicJigsawStructure} structure type,
 * similarly to how vanilla jigsaw structures use template pool files.
 * The primary difference and benefit is that dynamic jigsaw structures/pools allow jigsaw pieces to randomize their bounding boxes.
 * Due to the api differences involved, they are not compatible with template pool files,
 * however they do support structure templates (nbt structures), see {@link StructureTemplateDynamicJigsawElement}.
 * @param fallback Optional alternate pool to use at the final jigsaw depth or if all elements of this pool fail to generate
 * @param elements WeightedList of DynamicJigsawElements
 */
public record DynamicJigsawPool(
	Optional<Holder<DynamicJigsawPool>> fallback,
	WeightedList<DynamicJigsawElement> elements)
{
	/** structurebuddy:dynamic_jigsaw_pool / structurebuddy:empty - special pool provided by StructureBuddy which has no elements. This should be used instead of making an empty pool yourself  */
	public static final ResourceKey<DynamicJigsawPool> EMPTY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, StructureBuddy.id("empty"));
	
	/**
	 * e.g.
	<pre>
	{
		"fallback": "yourmod:your_fallback_pool", // id of another dynamic_jigsaw_pool file, defaults to structurebuddy:empty if not present
		"elements": [ // list of weighted element objects
			{
				"weight": 1, // positive int, must be present
				"data": {
					// see {@link DynamicJigsawElement#CODEC}
				}
			}
		]
	}
	</pre>
	 */
	public static final Codec<DynamicJigsawPool> DIRECT_CODEC = Codec.recursive(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL.location().toString(), directCodec -> RecordCodecBuilder.create(builder -> builder.group(
		RegistryFileCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, directCodec).optionalFieldOf("fallback").forGetter(DynamicJigsawPool::fallback),
		WeightedList.codec(DynamicJigsawElement.CODEC).fieldOf("elements").forGetter(DynamicJigsawPool::elements)
	).apply(builder, DynamicJigsawPool::new)));
	
	/** Holder Codec suitable for use in other datapack registry files */
	public static final Codec<Holder<DynamicJigsawPool>> CODEC = RegistryFileCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, DIRECT_CODEC);

	/**
	 * {@return Collection of DynamicJigsawElements in randomized order}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public Collection<? extends DynamicJigsawElement> getShuffledElements(RandomSource random)
	{
		List<DynamicJigsawElement> results = new ArrayList<>();
		
		WeightedList<DynamicJigsawElement> remainingWeightedList = this.elements;
		while (remainingWeightedList.unwrap().size() > 0)
		{
			DynamicJigsawElement selected = remainingWeightedList.getRandomOrThrow(random);
			results.add(selected);
			List<Weighted<DynamicJigsawElement>> unselected = new ArrayList<>();
			for (var weighted : remainingWeightedList.unwrap())
			{
				if (weighted.value() != selected)
				{
					unselected.add(weighted);
				}
			}
			remainingWeightedList = WeightedList.of(unselected);
		}
		
		return results;
	}
}
