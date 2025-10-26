package net.commoble.structurebuddy.api;

import net.minecraft.resources.ResourceLocation;

/**
 * APIs related to the mod
 */
public final class StructureBuddy
{
	private StructureBuddy() {}
	
	/** mod id **/
	public static final String MODID = "structurebuddy";

	/**
	 * Creates a ResourceLocation under the structurebuddy namespace
	 * @param path String path of returned ResourceLocation e.g. "foobar"
	 * @return ResourceLocation under the structuurebuddy namespace, e.g. "structurebuddy:foobar"
	 */
	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
