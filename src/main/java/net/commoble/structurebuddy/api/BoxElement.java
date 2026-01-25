package net.commoble.structurebuddy.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.structurebuddy.api.util.CodecBuddy;

/**
 * Elements of {@link BoxPool} files.
 * Responsible for producing BoxResults describing how bounding boxes are filled.
 */
public interface BoxElement
{
	/**
	 * Type-dispatched Codec. Subcodecs can be registered to {@link StructureBuddyRegistries#BOX_ELEMENT_TYPE}
	<pre>
	{
		"type": "yourmod:some_element_type",
		// additional fields as needed
	}
	</pre>
	 */
	public static final Codec<BoxElement> CODEC = CodecBuddy.dispatch(StructureBuddyRegistries.BOX_ELEMENT_TYPE, BoxElement::codec);
	
	/** {@return MapCodec for this type of BoxElement} */
	public abstract MapCodec<? extends BoxElement> codec();
	
	/**
	 * Responsible for defining the contents of some given BoundingBox (usually a region of some {@link DynamicJigsawElement}).
	 * If any randomness is to be applied that needs to be consistent across multiple chunks generating or a server reboot,
	 * this is where those random calls should be done.
	 * @param context BoxBakeContext
	 * @return BoxResult containing the size and contents of the structure piece to be added.
	 */
	public abstract BoxResult bake(BoxBakeContext context);
}
