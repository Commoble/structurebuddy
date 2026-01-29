package net.commoble.structurebuddy.api;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.JigsawBlockEntity.JointType;

/**
 * Overrides for jigsaw parameters, each individually optional
 * @param name Optional Identifier to override jigsaw name with
 * @param targetPool Optional ResourceKey to override target pool with
 * @param targetName Optional Identifier to override target jigsaw name with
 * @param jointType Optional JointType to override jigsaw joint type with
 * @param placementPriority Optional integer to override jigsaw placement priority with
 * @param selectionPriority Optional integer to override jigsaw selection priority with
 */
public record JigsawOverrides(
	Optional<Identifier> name,
	Optional<ResourceKey<DynamicJigsawPool>> targetPool,
	Optional<Identifier> targetName,
	Optional<JointType> jointType,
	Optional<Integer> placementPriority,
	Optional<Integer> selectionPriority
	)
{
	/**
	 * <pre>
	{
		"name": "yourmod:stairs_bottom",
		"target_pool": "yourmod:stairs_going_down",
		"target_name": "yourmod:stairs_top"
	}
	 * </pre>
	 */
	public static final Codec<JigsawOverrides> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Identifier.CODEC.optionalFieldOf("name").forGetter(JigsawOverrides::targetName),
			ResourceKey.codec(StructureBuddyRegistries.DYNAMIC_JIGSAW_POOL).optionalFieldOf("target_pool").forGetter(JigsawOverrides::targetPool),
			Identifier.CODEC.optionalFieldOf("target_name").forGetter(JigsawOverrides::targetName),
			JointType.CODEC.optionalFieldOf("joint_type").forGetter(JigsawOverrides::jointType),
			Codec.INT.optionalFieldOf("placement_priority").forGetter(JigsawOverrides::placementPriority),
			Codec.INT.optionalFieldOf("selection_priority").forGetter(JigsawOverrides::selectionPriority)
		).apply(builder, JigsawOverrides::new));
	
	/// ```json
	/// {
	/// 	"jigsaw:name": {
	/// 		// jigsaw overrides for jigsaws named jigsaw_name
	/// 	},
	/// 	"another:jigsaw": {
	/// 		// jigsaw overrides for jigsaws named another:jigsaw
	/// 	}
	/// }
	/// ```
	/// 
	public static final Codec<Map<Identifier, JigsawOverrides>> BY_JIGSAW_NAME_CODEC = Codec.unboundedMap(
		Identifier.CODEC,
		JigsawOverrides.CODEC);
}
