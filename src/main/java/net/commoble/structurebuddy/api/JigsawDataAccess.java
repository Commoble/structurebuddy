package net.commoble.structurebuddy.api;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.context.ContextKey;

/**
 * Read-write data access to jigsaw data, provided to baked pieces once they have been selected for placement in the jigsaw tree.
 * Be wary of adding the same objects to both global and branch data, or of mutating objects in-place,
 * as doing both can cause objects in the branch data to be modified by jigsaw pieces on other branches.
 */
public interface JigsawDataAccess extends JigsawDataReader
{
	/**
	 * Inserts or updates data under the given key to be available to any future jigsaw element baked into this structure.
	 * @param <T> Type of data to set
	 * @param key ContextKey to set the data under
	 * @param data Data to set under the given key
	 * @return T which previously existed under the given key, if any; null otherwise
	 */
	public <T> @Nullable T setGlobalData(ContextKey<T> key, T data);
	
	/**
	 * Inserts or updates data under the given key to be available to any future child
	 * (including grandchildren and furtherly indirect children) element baked into this structure.
	 * @param <T> Type of data to set
	 * @param key ContextKey to set the data under
	 * @param data Data to set under the given key
	 * @return T which previously existed under the given key, if any; null otherwise
	 */
	public <T> @Nullable T setBranchData(ContextKey<T> key, T data);
}
