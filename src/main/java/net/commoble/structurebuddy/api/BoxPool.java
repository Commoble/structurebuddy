package net.commoble.structurebuddy.api;

import java.util.Collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.util.RandomBuddy;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;

/**
 * BoxPools are defined in JSON files in the structurebuddy:box_pool datapack registry.
 * (i.e. data/yourmodid/structurebuddy/box_pool/yourfile.json)
 * These are intended to be used by structures which generate random elements inside BoundingBoxes of a known size.
 * @param elements WeightedList of BoxElements
 * @param delegates WeightedList of sub-BoxPools to include elements of. Weights of sub-pool elements are multiplied by sub-pool weight.
 */
public record BoxPool(
	WeightedList<BoxElement> elements,
	WeightedList<Holder<BoxPool>> delegates
	)
{
	/** structurebuddy:box_pool / structurebuddy:empty - special pool provided by StructureBuddy which has no elements. This should be used instead of making an empty pool yourself  */
	public static final ResourceKey<BoxPool> EMPTY = ResourceKey.create(StructureBuddyRegistries.BOX_POOL, StructureBuddy.id("empty"));
	
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
	public static final Codec<BoxPool> DIRECT_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(builder -> builder.group(
			WeightedList.codec(BoxElement.CODEC).optionalFieldOf("elements", WeightedList.of()).forGetter(BoxPool::elements),
			WeightedList.codec(BoxPool.CODEC).optionalFieldOf("delegates", WeightedList.of()).forGetter(BoxPool::delegates)
		).apply(builder, BoxPool::new)));
	
	/** Holder Codec suitable for use in other datapack registry files */
	public static final Codec<Holder<BoxPool>> CODEC = RegistryFileCodec.create(StructureBuddyRegistries.BOX_POOL, DIRECT_CODEC);
	
	/**
	 * {@return WeightedList of all BoxElements in this pool including from delegates, if any}
	 */
	public WeightedList<BoxElement> getElements()
	{
		WeightedList.Builder<BoxElement> builder = WeightedList.builder();
		builder.addAll(this.elements);
		for (var holder : this.delegates.unwrap())
		{
			int baseWeight = holder.weight();
			for (var element : holder.value().value().getElements().unwrap())
			{
				builder.add(element.value(), element.weight() * baseWeight);
			}
		}
		return builder.build();
	}

	/**
	 * {@return Collection of DynamicJigsawElements in randomized order}
	 * @param random RandomSource suitable for RNG during worldgen
	 */
	public Collection<? extends BoxElement> getShuffledElements(RandomSource random)
	{
		return RandomBuddy.shuffleWeightedList(this.getElements(), random);
	}
}
