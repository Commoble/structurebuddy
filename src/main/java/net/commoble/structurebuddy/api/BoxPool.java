package net.commoble.structurebuddy.api;

import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
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
 * BoxPools are defined in JSON files in the structurebuddy:box_pool datapack registry.
 * (i.e. data/yourmodid/structurebuddy/box_pool/yourfile.json)
 * These are intended to be used by structures which generate random elements inside BoundingBoxes of a known size.
 * @param elements WeightedList of BoxElement HolderSets. For holdersets with multiple elements, each element receives the given weight.
 * @param delegates WeightedList of sub-BoxPools to include elements of. Weights of sub-pool elements are multiplied by sub-pool weight.
 * @param combinedElements memoized WeightedList of all elements including delegates'
 */
public record BoxPool(
	WeightedList<HolderSet<BoxElement>> elements,
	WeightedList<HolderSet<BoxPool>> delegates,
	Supplier<WeightedList<Holder<BoxElement>>> combinedElements
	)
{
	/** structurebuddy:box_pool / structurebuddy:empty - special pool provided by StructureBuddy which has no elements. This should be used instead of making an empty pool yourself  */
	public static final ResourceKey<BoxPool> EMPTY = ResourceKey.create(StructureBuddyRegistries.BOX_POOL, StructureBuddy.id("empty"));
	
	/**
	 * e.g.
	<pre>
	{
		"delegates": [ // optional list of weighted pool holdersets to delegate to
			{
				"data": "#modid:pool_tag_id", // can be a pool id, list of pool ids, or tag hashtag
				"weight": 2 // multiplies weight of each weighted element in subpool by 2
			}
		],
		"elements": [ // optional list of weighted box_element holdersets
			{
				"data': "#modid:element_tag", // can refer to an element id, list of element ids, or tag hashtag
				"weight": 1 // all elements in tag are assigned individual weights of 1
			} 
		]
	}
	</pre>
	 */
	public static final Codec<BoxPool> DIRECT_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(builder -> builder.group(
			WeightedList.codec(BoxElement.HOLDERSET_CODEC).optionalFieldOf("elements", WeightedList.of()).forGetter(BoxPool::elements),
			WeightedList.codec(BoxPool.HOLDERSET_CODEC).optionalFieldOf("delegates", WeightedList.of()).forGetter(BoxPool::delegates)
		).apply(builder, BoxPool::new)));
	
	/** Holder Codec suitable for use in other datapack registry files */
	public static final Codec<Holder<BoxPool>> CODEC = RegistryFileCodec.create(StructureBuddyRegistries.BOX_POOL, DIRECT_CODEC);
	
	/** HolderSet Codec for BoxPools **/
	public static final Codec<HolderSet<BoxPool>> HOLDERSET_CODEC = HolderSetCodec.create(StructureBuddyRegistries.BOX_POOL, CODEC, false);

	/**
	 * Constructs a BoxPool from data available in json
	 * @param elements WeightedList of BoxElements
	 * @param delegates WeightedList of sub-BoxPools to include elements of. Weights of sub-pool elements are multiplied by sub-pool weight.
	 */
	public BoxPool(
		WeightedList<HolderSet<BoxElement>> elements,
		WeightedList<HolderSet<BoxPool>> delegates)
	{
		this(elements, delegates, Suppliers.memoize(() -> combineElements(elements, delegates)));
	}
	
	/**
	 * {@return WeightedList of primary elements}
	 * @deprecated use {@link BoxPool#combinedElements} to get all elements including delegates'
	 */
	@Deprecated
	public WeightedList<HolderSet<BoxElement>> elements()
	{
		return this.elements;
	}
	
	/**
	 * {@return WeightedList of all BoxElements in this pool including from delegates, if any}
	 * @param elements WeightedList of BoxElements
	 * @param delegates WeightedList of sub-BoxPools to include elements of. Weights of sub-pool elements are multiplied by sub-pool weight.
	 */
	public static WeightedList<Holder<BoxElement>> combineElements(WeightedList<HolderSet<BoxElement>> elements, WeightedList<HolderSet<BoxPool>> delegates)
	{
		Reference2IntMap<Holder<BoxElement>> weightMap = new Reference2IntOpenHashMap<>();
		for (Weighted<HolderSet<BoxElement>> weighted : elements.unwrap())
		{
			int weight = weighted.weight();
			for (Holder<BoxElement> holder : weighted.value())
			{
				weightMap.mergeInt(holder, weight, Math::addExact);
			}
		}
		for (Weighted<HolderSet<BoxPool>> weightedPool : delegates.unwrap())
		{
			int baseWeight = weightedPool.weight();
			for (Holder<BoxPool> poolHolder : weightedPool.value())
			{
				for (Weighted<Holder<BoxElement>> weightedElement : poolHolder.value().combinedElements.get().unwrap())
				{
					weightMap.mergeInt(weightedElement.value(), weightedElement.weight() * baseWeight, Math::addExact);
				}
			}
		}
		return WeightedList.of(weightMap.reference2IntEntrySet().stream().map(entry -> new Weighted<>(entry.getKey(), entry.getIntValue())).toList());
	}

	/**
	 * {@return Collection of DynamicJigsawElements in randomized order}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public List<Holder<BoxElement>> getShuffledElements(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(this.combinedElements().get(), random);
	}
}
