package net.commoble.structurebuddy.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.content.DynamicJigsawStructure;
import net.commoble.structurebuddy.api.content.StructureTemplateDynamicJigsawElement;
import net.commoble.structurebuddy.api.util.RandomBuddy;
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
 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
 * @param keepDelegateFallbacks boolean indicating whether to include fallbacks from delegates, if any
 */
public record DynamicJigsawPool(
	Optional<Holder<DynamicJigsawPool>> fallback,
	WeightedList<DynamicJigsawElement> elements,
	WeightedList<Holder<DynamicJigsawPool>> delegates,
	boolean keepDelegateFallbacks)
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
	public static final Codec<DynamicJigsawPool> DIRECT_CODEC = Codec.recursive(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL.identifier().toString(), directCodec -> RecordCodecBuilder.create(builder -> builder.group(
		RegistryFileCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, directCodec).optionalFieldOf("fallback").forGetter(DynamicJigsawPool::fallback),
		WeightedList.codec(DynamicJigsawElement.CODEC).optionalFieldOf("elements", WeightedList.of()).forGetter(DynamicJigsawPool::elements),
		WeightedList.codec(DynamicJigsawPool.CODEC).optionalFieldOf("delegates", WeightedList.of()).forGetter(DynamicJigsawPool::delegates),
		Codec.BOOL.optionalFieldOf("keep_delegate_fallbacks", false).forGetter(DynamicJigsawPool::keepDelegateFallbacks)
	).apply(builder, DynamicJigsawPool::new)));
	
	/** Holder Codec suitable for use in other datapack registry files */
	public static final Codec<Holder<DynamicJigsawPool>> CODEC = RegistryFileCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, DIRECT_CODEC);

	/**
	 * {@return WeightedList of all DynamicJigsawElements in this pool including from delegates, if any}
	 */
	public List<Weighted<DynamicJigsawElement>> getElements()
	{

		List<Weighted<DynamicJigsawElement>> list = new ArrayList<>();
		list.addAll(this.elements.unwrap());
		for (var poolHolder : this.delegates.unwrap())
		{
			int baseWeight = poolHolder.weight();
			for (var subElement : poolHolder.value().value().getElements())
			{
				list.add(new Weighted<>(subElement.value(), subElement.weight() * baseWeight));
			}
		}
		return list;
	}
	
	/**
	 * {@return WeightedList of fallback elements (including from delegates if this.keepDelegateFallbacks is true)}
	 */
	public List<Weighted<DynamicJigsawElement>> getFallbacks()
	{
		List<Weighted<DynamicJigsawElement>> list = new ArrayList<>();
		this.fallback.ifPresent(holder -> {
			list.addAll(holder.value().getElements());
		});
		if (this.keepDelegateFallbacks)
		{
			for (var poolHolder : this.delegates.unwrap())
			{
				int baseWeight = poolHolder.weight();
				for (var subElement : poolHolder.value().value().getFallbacks())
				{
					list.add(new Weighted<>(subElement.value(), subElement.weight() * baseWeight));
				}
			}
		}
		return list;
	}
	
	/**
	 * {@return Collection of fallbacks, shuffled (including fallbacks from delegates of this.keepDelegateFallbacks is true)}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public Collection<? extends DynamicJigsawElement> getShuffledFallbacks(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(WeightedList.of(this.getFallbacks()), random);
	}
	
	/**
	 * {@return Collection of DynamicJigsawElements in randomized order}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public Collection<? extends DynamicJigsawElement> getShuffledElements(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(WeightedList.of(this.getElements()), random);
	}
}
