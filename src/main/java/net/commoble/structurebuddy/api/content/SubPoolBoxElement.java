package net.commoble.structurebuddy.api.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.BoxBakeContext;
import net.commoble.structurebuddy.api.BoxElement;
import net.commoble.structurebuddy.api.BoxPool;
import net.commoble.structurebuddy.api.BoxResult;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.commoble.structurebuddy.api.util.BoxBuddy;
import net.commoble.structurebuddy.api.util.RandomBuddy;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * BoxElement which rolls a different box pool to produce results from.
 * @param value Holder of BoxPool to roll and produce a result from
 * @param expand if true, attempts every element of subpool (in weighted random order) until a valid result is found (or list is exhausted)
 */
public record SubPoolBoxElement(Holder<BoxPool> value, boolean expand) implements BoxElement
{
	/** structurebuddy:box_element_type / structurebuddy:subpool */
	public static final ResourceKey<MapCodec<? extends BoxElement>> KEY = ResourceKey.create(StructureBuddyRegistries.BOX_ELEMENT_TYPE, StructureBuddy.id("subpool"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends BoxElement>, MapCodec<SubPoolBoxElement>> HOLDER = DeferredHolder.create(KEY);
	/**
	 * e.g.
	 <pre>
	 {
	 	"type": "structurebuddy:subpool",
	 	"value": "yourmod:some_pool", // box_pool file to roll
	 	"expand": false, // defaults false; if true, attempts every sub-element
	 }
	 </pre>
	 */
	public static final MapCodec<SubPoolBoxElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			BoxPool.CODEC.fieldOf("value").forGetter(SubPoolBoxElement::value),
			Codec.BOOL.optionalFieldOf("expand", false).forGetter(SubPoolBoxElement::expand)
		).apply(builder, SubPoolBoxElement::new));
	
	@Override
	public MapCodec<? extends BoxElement> codec()
	{
		return CODEC;
	}
	@Override
	public BoxResult bake(BoxBakeContext context)
	{
		if (this.expand)
		{
			for (BoxElement element : RandomBuddy.shuffleWeightedList(this.value.value().elements(), context.generationContext().random()))
			{
				BoxResult result = element.bake(context);
				BoundingBox childBox = result.localBoundingBox();
				if (BoxBuddy.fitsWithin(childBox, context.box()))
				{
					return result;
				}
			}
			return BoxResult.invalid();
		}
		else {
			return this.value
				.value()
				.elements()
				.getRandom(context.generationContext().random())
				.map(element -> element.bake(context))
				.orElse(BoxResult.invalid());
		}
	}
}
