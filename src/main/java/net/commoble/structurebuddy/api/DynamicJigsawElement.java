package net.commoble.structurebuddy.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.util.CodecBuddy;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;

/**
 * Elements of {@link DynamicJigsawPool} files.
 * Responsible for producing DynamicJigsawResults describing pieces which may be added to the jigsaw tree.
 */
public interface DynamicJigsawElement
{
	/**
	 * Type-dispatched Codec. Subcodecs can be registered to {@link StructureBuddyRegistries#DYNAMIC_JIGSAW_ELEMENT_TYPE}
	<pre>
	{
		"type": "yourmod:some_element_type",
		// additional fields as needed
	}
	</pre>
	 */
	public static final Codec<DynamicJigsawElement> DIRECT_CODEC = CodecBuddy.dispatch(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT_TYPE, DynamicJigsawElement::codec);
	
	/** Codec to load DynamicJigsawElement holders by id, for use in other datapack registry files **/
	public static final Codec<Holder<DynamicJigsawElement>> CODEC = RegistryFileCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT, DIRECT_CODEC);
	
	/** HolderSet Codec for DynamicJigsawElements **/
	public static final Codec<HolderSet<DynamicJigsawElement>> HOLDERSET_CODEC = HolderSetCodec.create(StructureBuddyRegistries.DYNAMIC_JIGSAW_ELEMENT, CODEC, false);
	
	/**
	 * {@return MapCodec for this type of DynamicJigsawElement, which has been registered to {@link StructureBuddyRegistries#DYNAMIC_JIGSAW_ELEMENT_TYPE}} 
	 */
	public abstract MapCodec<? extends DynamicJigsawElement> codec();
	
	/**
	 * This method is responsible for defining the contents and connections of a potential structure piece to be added to the jigsaw tree.
	 * If any randomness is to be applied that needs to be consistent across multiple chunks generating or a server reboot,
	 * this is where those random calls should be done.
	 * @param context DynamicJigsawBakeContext
	 * @return DynamicJigsawResult containing the size, contents, and connections of the structure piece to be added.
	 */
	public abstract DynamicJigsawResult bake(DynamicJigsawBakeContext context);
}
