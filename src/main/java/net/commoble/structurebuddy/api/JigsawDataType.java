package net.commoble.structurebuddy.api;

import java.util.Map;

import com.mojang.serialization.Codec;

import net.commoble.structurebuddy.api.content.DynamicJigsawStructure;
import net.commoble.structurebuddy.api.util.CodecBuddy;

/// JigsawDataTypes are registered via {@link StructureBuddyRegistries#JIGSAW_DATA_TYPE}.
/// 
/// They are used to share arbitary serializable data between pieces in {@link DynamicJigsawStructure}s.
/// 
/// For example, a piece could specify that it and all supported child pieces 
/// should share the same building material,
/// or a piece could track how many copies of itself have been generated
/// and refuse to generate more of itself after three pieces have been selected.
/// @param <T> Type of data to serialize
public final class JigsawDataType<T>
{
	/// Serializes a map where each key is a JigsawDataType
	/// and each value is serialized by that type's codec:
	/// ```json
	/// {
	/// 	"modid:some_registered_data_type": {
	/// 		// whatever
	/// 	},
	/// 	// can also serialize primitive values
	/// 	"modid:another_registered_data_type": 5
	/// }
	/// ```
	public static final Codec<Map<JigsawDataType<?>, Object>> MAP_CODEC = Codec.dispatchedMap(
		CodecBuddy.registryCodec(StructureBuddyRegistries.JIGSAW_DATA_TYPE),
		JigsawDataType::codec);
	
	private final Codec<T> codec;
	
	/// It's a constructor!
	/// @param codec Codec to serialize data with
	public JigsawDataType (Codec<T> codec)
	{
		this.codec = codec;
	}
	
	///{@return the codec}
	public Codec<T> codec()
	{
		return this.codec;
	}
}
