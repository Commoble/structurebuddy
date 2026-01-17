package net.commoble.structurebuddy.api;

import net.minecraft.resources.Identifier;

/**
 * APIs related to the mod
 */
public final class StructureBuddy
{
	private StructureBuddy() {}
	
	/** mod id **/
	public static final String MODID = "structurebuddy";

	/**
	 * Creates a Identifier under the structurebuddy namespace
	 * @param path String path of returned Identifier e.g. "foobar"
	 * @return Identifier under the structuurebuddy namespace, e.g. "structurebuddy:foobar"
	 */
	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
}
