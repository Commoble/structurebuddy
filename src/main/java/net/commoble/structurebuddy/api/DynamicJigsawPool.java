package net.commoble.structurebuddy.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.commoble.structurebuddy.api.content.DynamicJigsawStructure;
import net.commoble.structurebuddy.api.content.StructureTemplateDynamicJigsawElement;
import net.commoble.structurebuddy.api.util.RandomBuddy;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.HolderSetCodec;
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
 * @param fallback Optional HolderSet of alternate pools to use at the final jigsaw depth or if all elements of this pool fail to generate.
 * @param elements WeightedList of DynamicJigsawElement HolderSets. For HolderSets with multiple elements, each will receive the given weight.
 * @param delegates WeightedList of HolderSets of sub-pools to pull elements from. For HolderSets with multiple elements, each pool receives the same weight. Weights of pools and pools' sub-elements are multiplied together.
 * @param combinedElements memoized WeightedList of all elements both from this pool's primary elements and each of its delegates
 * @param combinedFallbacks memoized WeightedList of fallback elements (including from delegates if requested by keepDelegateFallbacks)
 * @param trimmedFallbacks memoized WeightedList of fallback elementst, excluding elements from combinedElements
 */
public record DynamicJigsawPool(
	Optional<HolderSet<DynamicJigsawPool>> fallback,
	WeightedList<HolderSet<DynamicJigsawElement>> elements,
	WeightedList<HolderSet<DynamicJigsawPool>> delegates,
	Supplier<WeightedList<Holder<DynamicJigsawElement>>> combinedElements,
	Supplier<WeightedList<Holder<DynamicJigsawElement>>> combinedFallbacks,
	Supplier<WeightedList<Holder<DynamicJigsawElement>>> trimmedFallbacks)
{
	/** structurebuddy:dynamic_jigsaw_pool / structurebuddy:empty - special pool provided by StructureBuddy which has no elements. This should be used instead of making an empty pool yourself  */
	public static final ResourceKey<DynamicJigsawPool> EMPTY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, StructureBuddy.id("empty"));
	
	/**
	 * Constructs a BoxPool from data available in json
	 * @param fallback Optional alternate pool to use at the final jigsaw depth or if all elements of this pool fail to generate
	 * @param elements WeightedList of DynamicJigsawElements. Use {@link DynamicJigsawPool#combinedElements} to get combined primary and delegate elements.
	 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
	 */
	public DynamicJigsawPool(
		Optional<HolderSet<DynamicJigsawPool>> fallback,
		WeightedList<HolderSet<DynamicJigsawElement>> elements,
		WeightedList<HolderSet<DynamicJigsawPool>> delegates)
	{
		this(fallback, elements, delegates,
			Suppliers.memoize(() -> combineElements(elements, delegates)),
			Suppliers.memoize(() -> combineFallbacks(fallback)),
			Suppliers.memoize(() -> trimFallbacks(fallback, elements, delegates)));
	}
	
	/**
	 * {@return fallback pool}
	 * @deprecated use {@link DynamicJigsawPool#combinedFallbacks} to get all fallbacks including from delegates if permitted
	 */
	@Deprecated
	public Optional<HolderSet<DynamicJigsawPool>> fallback()
	{
		return this.fallback;
	}
	
	/**
	 * {@return WeightedList of DynamicJigsawElement HolderSets}
	 * @deprecated Use {@link DynamicJigsawPool#combinedElements()} to get combined primary and delegate elements
	 */
	@Deprecated
	public WeightedList<HolderSet<DynamicJigsawElement>> elements()
	{
		return this.elements;
	}
	
	/**
	 * e.g.
	<pre>
	{
		"fallback": "yourmod:your_fallback_pool", // id of another dynamic_jigsaw_pool file, defaults to structurebuddy:empty if not present, can use #tag id to use elements from all pools in tag as fallbacks (fallbacks of fallback pools are ignored)
		"delegates": [ // optional list of weighted pool holdersets to delegate to
			{
				"data": "#modid:pool_tag_id", // can be a pool id, list of pool ids, or tag hashtag
				"weight": 2 // multiplies weight of each weighted element in subpool by 2
			}
		],
		"elements": [ // optional list of weighted dynamic_jigsaw_element holdersets
			{
				"data': "#modid:element_tag", // can refer to an element id, list of element ids, or tag hashtag
				"weight": 1 // all elements in tag are assigned individual weights of 1
			} 
		]
	}
	</pre>
	 */
	public static final Codec<DynamicJigsawPool> DIRECT_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(builder -> builder.group(
		DynamicJigsawPool.HOLDERSET_CODEC.optionalFieldOf("fallback").forGetter(DynamicJigsawPool::fallback),
		WeightedList.codec(DynamicJigsawElement.HOLDERSET_CODEC).optionalFieldOf("elements", WeightedList.of()).forGetter(DynamicJigsawPool::elements),
		WeightedList.codec(DynamicJigsawPool.HOLDERSET_CODEC).optionalFieldOf("delegates", WeightedList.of()).forGetter(DynamicJigsawPool::delegates)
	).apply(builder, DynamicJigsawPool::new)));
	
	/** Holder Codec suitable for use in other datapack registry files */
	public static final Codec<Holder<DynamicJigsawPool>> CODEC = RegistryFileCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, DIRECT_CODEC);
	
	/** HolderSet Codec for DynamicJigsawPools **/
	public static final Codec<HolderSet<DynamicJigsawPool>> HOLDERSET_CODEC = HolderSetCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL, CODEC, false);

	/**
	 * {@return WeightedList of all DynamicJigsawElements combined from primary elements and delegate pools}
	 * @param elements WeightedList of DynamicJigsawElements.
	 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
	 */
	public static WeightedList<Holder<DynamicJigsawElement>> combineElements(WeightedList<HolderSet<DynamicJigsawElement>> elements, WeightedList<HolderSet<DynamicJigsawPool>> delegates)
	{
		Reference2IntMap<Holder<DynamicJigsawElement>> weightMap = new Reference2IntOpenHashMap<>();
		for (Weighted<HolderSet<DynamicJigsawElement>> weightedHolderSet : elements.unwrap())
		{
			int baseWeight = weightedHolderSet.weight();
			for (Holder<DynamicJigsawElement> holder : weightedHolderSet.value())
			{
				weightMap.merge(holder, baseWeight, Math::addExact);
			}
		}
		for (Weighted<HolderSet<DynamicJigsawPool>> weightedPool : delegates.unwrap())
		{
			int baseWeight = weightedPool.weight();
			for (Holder<DynamicJigsawPool> poolHolder : weightedPool.value())
			{
				for (Weighted<Holder<DynamicJigsawElement>> weightedElement : poolHolder.value().combinedElements().get().unwrap())
				{
					weightMap.merge(weightedElement.value(), weightedElement.weight() * baseWeight, Math::addExact);
				}
			}
		}

		return WeightedList.of(weightMap.reference2IntEntrySet().stream().map(entry -> new Weighted<>(entry.getKey(), entry.getIntValue())).toList());
	}
	
	/**
	 * {@return WeightedList of fallback elements}
	 * @param fallback Optional alternate pool to use at the final jigsaw depth or if all elements of this pool fail to generate
	 */
	public static WeightedList<Holder<DynamicJigsawElement>> combineFallbacks(Optional<HolderSet<DynamicJigsawPool>> fallback)
	{
		Reference2IntMap<Holder<DynamicJigsawElement>> weightMap = new Reference2IntOpenHashMap<>();
		for (Holder<DynamicJigsawPool> poolHolder : fallback.orElse(HolderSet.empty()))
		{
			for (Weighted<Holder<DynamicJigsawElement>> weightedElement : poolHolder.value().combinedElements.get().unwrap())
			{
				weightMap.mergeInt(weightedElement.value(), weightedElement.weight(), Math::addExact);
			}
		}
		return WeightedList.of(weightMap.reference2IntEntrySet().stream().map(entry -> new Weighted<>(entry.getKey(), entry.getIntValue())).toList());
	}
	
	/**
	 * {@return WeightedList of fallback elements, excluding elements in primary element set}
	 * @param fallback Optional alternate pool to use at the final jigsaw depth or if all elements of this pool fail to generate
	 * @param elements WeightedList of DynamicJigsawElements.
	 * @param delegates WeightedList of sub-pools to pull elements from. Weights of pools and sub-elements are multiplied together.
	 */
	public static WeightedList<Holder<DynamicJigsawElement>> trimFallbacks(Optional<HolderSet<DynamicJigsawPool>> fallback, WeightedList<HolderSet<DynamicJigsawElement>> elements, WeightedList<HolderSet<DynamicJigsawPool>> delegates)
	{
		WeightedList<Holder<DynamicJigsawElement>> nominalFallbacks = combineFallbacks(fallback);
		WeightedList<Holder<DynamicJigsawElement>> primaryElements = combineElements(elements, delegates);
		Set<Holder<DynamicJigsawElement>> primarySet = primaryElements.unwrap().stream().map(Weighted::value).collect(Collectors.toSet());
		return WeightedList.of(nominalFallbacks.unwrap().stream().filter(weighted -> !primarySet.contains(weighted.value())).toList());
	}
	
	/**
	 * {@return Collection of fallbacks, shuffled (including fallbacks from delegates of this.keepDelegateFallbacks is true)}
	 * @param random RandomSource suitable for RNG during worldgen
	 * @param skipPrimaryElements If true, will not include fallback elements which are also included in the non-fallback elements
	 */
	public List<Holder<DynamicJigsawElement>> getShuffledFallbacks(RandomSource random, boolean skipPrimaryElements)
	{
		WeightedList<Holder<DynamicJigsawElement>> fallbackList = skipPrimaryElements
			? this.trimmedFallbacks.get()
			: this.combinedFallbacks.get();
		return RandomBuddy.shuffleWeightedList(fallbackList, random);
	}
	
	/**
	 * {@return Collection of DynamicJigsawElements in randomized order}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public List<Holder<DynamicJigsawElement>> getShuffledElements(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(this.combinedElements().get(), random);
	}
}
