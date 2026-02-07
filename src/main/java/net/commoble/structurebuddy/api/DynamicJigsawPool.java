package net.commoble.structurebuddy.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Suppliers;
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
 * @param elements WeightedList of DynamicJigsawElements. Use {@link DynamicJigsawPool#combinedElements} to get combined primary and delegate elements.
 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
 * @param keepDelegateFallbacks boolean indicating whether to include fallbacks from delegates, if any
 * @param combinedElements memoized WeightedList of all elements both from this pool's primary elements and each of its delegates
 * @param combinedFallbacks memoized WeightedList of fallback elements (including from delegates if requested by keepDelegateFallbacks)
 */
public record DynamicJigsawPool(
	Optional<Holder<DynamicJigsawPool>> fallback,
	WeightedList<DynamicJigsawElement> elements,
	WeightedList<Holder<DynamicJigsawPool>> delegates,
	boolean keepDelegateFallbacks,
	Supplier<WeightedList<DynamicJigsawElement>> combinedElements,
	Supplier<WeightedList<DynamicJigsawElement>> combinedFallbacks)
{
	/** structurebuddy:dynamic_jigsaw_pool / structurebuddy:empty - special pool provided by StructureBuddy which has no elements. This should be used instead of making an empty pool yourself  */
	public static final ResourceKey<DynamicJigsawPool> EMPTY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, StructureBuddy.id("empty"));
	
	/**
	 * Constructs a BoxPool from data available in json
	 * @param fallback Optional alternate pool to use at the final jigsaw depth or if all elements of this pool fail to generate
	 * @param elements WeightedList of DynamicJigsawElements. Use {@link DynamicJigsawPool#combinedElements} to get combined primary and delegate elements.
	 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
	 * @param keepDelegateFallbacks boolean indicating whether to include fallbacks from delegates, if any
	 */
	public DynamicJigsawPool(
		Optional<Holder<DynamicJigsawPool>> fallback,
		WeightedList<DynamicJigsawElement> elements,
		WeightedList<Holder<DynamicJigsawPool>> delegates,
		boolean keepDelegateFallbacks)
	{
		this(fallback, elements, delegates, keepDelegateFallbacks,
			Suppliers.memoize(() -> combineElements(elements, delegates, new HashSet<>())),
			Suppliers.memoize(() -> combineFallbacks(fallback, delegates, keepDelegateFallbacks, new HashSet<>())));
	}
	
	/**
	 * {@return WeightedList of DynamicJigsawElements}
	 * @deprecated Use {@link DynamicJigsawPool#combinedElements()} to get combined primary and delegate elements
	 */
	@Deprecated
	public WeightedList<DynamicJigsawElement> elements()
	{
		return this.elements;
	}
	
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
	 * {@return WeightedList of all DynamicJigsawElements combined from primary elements and delegate pools}
	 * @param elements WeightedList of DynamicJigsawElements.
	 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
	 * @param seenPools Set of pool keys already seen, to avoid duplicates and circular references
	 */
	public static WeightedList<DynamicJigsawElement> combineElements(WeightedList<DynamicJigsawElement> elements, WeightedList<Holder<DynamicJigsawPool>> delegates, Set<ResourceKey<DynamicJigsawPool>> seenPools)
	{
		List<Weighted<DynamicJigsawElement>> list = new ArrayList<>();
		list.addAll(elements.unwrap());
		for (Weighted<Holder<DynamicJigsawPool>> weighted : delegates.unwrap())
		{
			Holder<DynamicJigsawPool> holder = weighted.value();
			@Nullable ResourceKey<DynamicJigsawPool> key = holder.unwrapKey().orElse(null);
			if (key != null)
			{
				if (seenPools.contains(key))
				{
					continue; // don't add again
				}
				seenPools.add(key);
			}
			int baseWeight = weighted.weight();
			for (Weighted<DynamicJigsawElement> subElement : holder.value().combinedElements().get().unwrap())
			{
				list.add(new Weighted<>(subElement.value(), subElement.weight() * baseWeight));
			}
		}
		return WeightedList.of(list);
	}
	
	/**
	 * {@return WeightedList of fallback elements (including from delegates if this.keepDelegateFallbacks is true)}
	 * @param fallback Optional alternate pool to use at the final jigsaw depth or if all elements of this pool fail to generate
	 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
	 * @param keepDelegateFallbacks boolean indicating whether to include fallbacks from delegates, if any
	 * @param seenPools Set of pool ids already seen, to avoiod duplicates and circular references
	 */
	public static WeightedList<DynamicJigsawElement> combineFallbacks(Optional<Holder<DynamicJigsawPool>> fallback, WeightedList<Holder<DynamicJigsawPool>> delegates, boolean keepDelegateFallbacks, Set<ResourceKey<DynamicJigsawPool>> seenPools)
	{
		List<Weighted<DynamicJigsawElement>> list = new ArrayList<>();
		fallback.ifPresent(holder -> {
			list.addAll(holder.value().combinedElements().get().unwrap());
		});
		if (keepDelegateFallbacks)
		{
			for (var weighted : delegates.unwrap())
			{
				Holder<DynamicJigsawPool> holder = weighted.value();
				@Nullable ResourceKey<DynamicJigsawPool> key = holder.unwrapKey().orElse(null);
				if (key != null)
				{
					if (seenPools.contains(key))
					{
						continue; // don't add again
					}
					seenPools.add(key);
				}
				int baseWeight = weighted.weight();
				for (var subElement : holder.value().combinedFallbacks().get().unwrap())
				{
					list.add(new Weighted<>(subElement.value(), subElement.weight() * baseWeight));
				}
			}
		}
		return WeightedList.of(list);
	}
	
	/**
	 * {@return Collection of fallbacks, shuffled (including fallbacks from delegates of this.keepDelegateFallbacks is true)}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public Collection<? extends DynamicJigsawElement> getShuffledFallbacks(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(this.combinedFallbacks().get(), random);
	}
	
	/**
	 * {@return Collection of DynamicJigsawElements in randomized order}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public Collection<? extends DynamicJigsawElement> getShuffledElements(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(this.combinedElements().get(), random);
	}
}
