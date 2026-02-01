package net.commoble.structurebuddy.api.content;

import java.util.EnumSet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.BoxBakeContext;
import net.commoble.structurebuddy.api.BoxElement;
import net.commoble.structurebuddy.api.BoxResult;
import net.commoble.structurebuddy.api.DynamicJigsawBakeContext;
import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.registries.DeferredHolder;

/// DynamicJigsawElement which delegates to a box element within a box of some random size
/// @param element BoxElement to generate in the generated box
/// @param xSize IntProvider to generate the width of the box
/// @param ySize IntProvider to generate the height of the box
/// @param zSize IntProvider to generate the length of the box
public record BoxDynamicJigsawElement(
	BoxElement element,
	IntProvider xSize,
	IntProvider ySize,
	IntProvider zSize) implements DynamicJigsawElement
{
	/** structurebuddy:dynamic_pool_element_type / structurebuddy:box */
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("empty"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends DynamicJigsawElement>, MapCodec<EmptyDynamicJigsawElement>> HOLDER = DeferredHolder.create(KEY);
	
	/// For example:
	/// ```json
	/// {
	/// 	"element": {
	/// 		// box element
	/// 	},
	/// 	"x_size": 5,
	/// 	"z_size": 5,
	/// 	"y_size": {
	/// 		"type": "uniform",
	/// 		"min_inclusive": 3,
	/// 		"max_inclusive": 10
	/// 	}
	/// }
	/// ```
	public static final MapCodec<BoxDynamicJigsawElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			BoxElement.CODEC.fieldOf("element").forGetter(BoxDynamicJigsawElement::element),
			IntProvider.CODEC.fieldOf("x_size").forGetter(BoxDynamicJigsawElement::xSize),
			IntProvider.CODEC.fieldOf("y_size").forGetter(BoxDynamicJigsawElement::ySize),
			IntProvider.CODEC.fieldOf("z_size").forGetter(BoxDynamicJigsawElement::zSize)
		).apply(builder, BoxDynamicJigsawElement::new));

	@Override
	public MapCodec<? extends DynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		RandomSource random = context.generationContext().random();
		BoundingBox box = new BoundingBox(
			0,
			0,
			0,
			this.xSize.sample(random),
			this.ySize.sample(random),
			this.zSize.sample(random));
		BoxResult boxResult = this.element.bake(new BoxBakeContext(
			context.generationContext(),
			box,
			EnumSet.allOf(Direction.class),
			context.data(),
			context.rotation(),
			context.liquidSettings()));
		return new DynamicJigsawResult(
			boxResult.pieceFillerFactory(),
			box,
			boxResult.connectionsToParent(),
			boxResult.connectionsToChildren(),
			boxResult.onSelected());
	}
}
