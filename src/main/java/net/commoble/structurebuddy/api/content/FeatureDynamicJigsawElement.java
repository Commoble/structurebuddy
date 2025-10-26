package net.commoble.structurebuddy.api.content;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.structurebuddy.api.DynamicJigsawElement;
import net.commoble.structurebuddy.api.DynamicJigsawFillContext;
import net.commoble.structurebuddy.api.DynamicJigsawResult;
import net.commoble.structurebuddy.api.JigsawConnectionToChild;
import net.commoble.structurebuddy.api.JigsawConnectionToParent;
import net.commoble.structurebuddy.api.PieceFiller;
import net.commoble.structurebuddy.api.StructureBuddy;
import net.commoble.structurebuddy.api.StructureBuddyRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * DynamicJigsawElement which places a ConfiguredFeature at the location of its jigsaw connection.
 * Roughly analogous to {@link FeaturePoolElement} but can connect to the parent piece more flexibly.
 * Still doesn't produce a bounding box around its feature to avoid overlap;
 * if a bounding box is desired, consider registering a DynamicJigsawElement type which generates the required blocks
 * @param feature Holder of a ConfiguredFeature to generate
 * @param jigsawName ResourceLocation of jigsaw connection to be targeted by parent, e.g. "minecraft:bottom"
 */
public record FeatureDynamicJigsawElement(Holder<ConfiguredFeature<?,?>> feature, ResourceLocation jigsawName) implements DynamicJigsawElement
{
	private static final Logger LOGGER = LogUtils.getLogger();
	
	/** structurebuddy:dynamic_jigsaw_element_type / structurebuddy:feature */
	public static final ResourceKey<MapCodec<? extends DynamicJigsawElement>> KEY = ResourceKey.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, StructureBuddy.id("feature"));
	/** holder */
	public static final DeferredHolder<MapCodec<? extends DynamicJigsawElement>, MapCodec<FeatureDynamicJigsawElement>> HOLDER = DeferredHolder.create(KEY);
	/** minecraft:bottom, same as what FeaturePoolElement uses */
	public static final ResourceLocation DEFAULT_JIGSAW_NAME = ResourceLocation.withDefaultNamespace("bottom"); // same as FeaturePoolElement
	
	/**
	 * e.g.
	 <pre>
	 {
	 	"type": "structurebuddy:feature",
	 	"feature": "yourmod:tree", // id of configured_feature file
	 	"jigsaw_name": "tree_feature" // optional, defaults to minecraft:bottom if not specified"
	 }
	 </pre>
	 */
	public static final MapCodec<FeatureDynamicJigsawElement> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			ConfiguredFeature.CODEC.fieldOf("feature").forGetter(FeatureDynamicJigsawElement::feature),
			ResourceLocation.CODEC.optionalFieldOf("jigsaw_name", DEFAULT_JIGSAW_NAME).forGetter(FeatureDynamicJigsawElement::jigsawName)
		).apply(builder, FeatureDynamicJigsawElement::new));
			
	@Override
	public MapCodec<? extends DynamicJigsawElement> codec()
	{
		return CODEC;
	}

	@Override
	public DynamicJigsawResult bake(DynamicJigsawBakeContext context)
	{
		@Nullable JigsawConnectionToChild parent = context.parent();
		if (parent == null)
		{
			LOGGER.warn("{} cannot be a root element", this);
			return DynamicJigsawResult.EMPTY;
		}
		FrontAndTop parentOrientation = parent.orientation();
		BlockPos childPos = parent.pos().relative(parentOrientation.front());
		PieceFiller pieceFiller = new FeaturePieceFiller(feature, childPos);
		BoundingBox boundingBox = BoundingBox.fromCorners(childPos, childPos);
		FrontAndTop childOrientation = FrontAndTop.fromFrontAndTop(parentOrientation.front().getOpposite(), parentOrientation.top());
		JigsawConnectionToParent jigsaw = new JigsawConnectionToParent(
			childPos,
			childOrientation,
			this.jigsawName);
		// would be nice if we could configure this to generate before or after other pieces
		// but placement priority only affects jigsaw assembling for child jigsaws, of which we have none, not adding-blocks-to-chunks
		return new DynamicJigsawResult(pieceFiller, boundingBox, List.of(jigsaw), List.of());
	}
	
	/**
	 * PieceFiller used by the feature element. Places a feature at the specified location.
	 * @param feature Holder of ConfiguredFeature to generate
	 * @param featurePos BlockPos in absolute world space to generate feature at
	 */
	public static record FeaturePieceFiller(Holder<ConfiguredFeature<?,?>> feature, BlockPos featurePos) implements PieceFiller
	{
		/** structurebuddy:piece_filler_type / structurebuddy:feature */
		public static final ResourceKey<MapCodec<? extends PieceFiller>> KEY = ResourceKey.create(StructureBuddyRegistries.PIECE_FILLER_TYPE, StructureBuddy.id("feature"));
		/** holder */
		public static final DeferredHolder<MapCodec<? extends PieceFiller>, MapCodec<FeaturePieceFiller>> HOLDER = DeferredHolder.create(KEY);
		/**
		 * e.g.
		 <pre>
		 {
		 	"feature": "yourmod:tree", // id of some configured_feature to generate
		 	"feature_pos": [1,2,3] // blockpos in absolute world space to generate feature at
		 }
		 </pre>
		 */
		// javac hates these generics for some reason, use explicit generics on the builder
		public static final MapCodec<? extends PieceFiller> CODEC = RecordCodecBuilder.<FeaturePieceFiller>mapCodec(builder -> builder.<Holder<ConfiguredFeature<?,?>>, BlockPos>group(
				ConfiguredFeature.CODEC.fieldOf("feature").forGetter(FeaturePieceFiller::feature),
				BlockPos.CODEC.fieldOf("feature_pos").forGetter(FeaturePieceFiller::featurePos)
			).apply(builder, FeaturePieceFiller::new));
		
		@Override
		public MapCodec<? extends PieceFiller> codec()
		{
			return CODEC;
		}

		@Override
		public void fill(DynamicJigsawFillContext context)
		{
			if (context.chunkBoundingBox().isInside(this.featurePos))
			{
				this.feature.value().place(context.level(), context.chunkGenerator(), context.random(), this.featurePos);
			}
		}
	}

}
