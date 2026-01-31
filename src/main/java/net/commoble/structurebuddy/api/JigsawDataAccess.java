package net.commoble.structurebuddy.api;

import org.jetbrains.annotations.Nullable;

/**
 * Read-write data access to jigsaw data, provided to baked pieces once they have been selected for placement in the jigsaw tree.
 * Objects in the jigsaw data must be considered immutable and not modified by any means..
 */
public interface JigsawDataAccess extends JigsawDataReader
{
	/**
	 * Inserts or updates data under the given key to be available to any future jigsaw element baked into this structure.
	 * @param <T> Type of data to set
	 * @param key JigsawDataType to set the data under
	 * @param data Data to set under the given key. Must not be a mutated object from existing data.
	 * If null, existing entry will be removed from the map, if present.
	 * @return T which previously existed under the given key, if any; null otherwise
	 */
	public abstract <T> @Nullable T setGlobalData(JigsawDataType<T> key, @Nullable T data);
	
	/**
	 * Removes data from the global data map.
	 * @param <T> Type of data to remove
	 * @param key JigsawDataType to remove
	 * @return T which previously existed under the given key, if any; null otherwise
	 */
	public default <T> @Nullable T removeGlobalData(JigsawDataType<T> key)
	{
		return this.setGlobalData(key, null);
	}
	
	/**
	 * Inserts or updates data under the given key to be available to any future child
	 * (including grandchildren and furtherly indirect children) element baked into this structure.
	 * @param <T> Type of data to set
	 * @param key JigsawDataType to set the data under
	 * @param data Data to set under the given key. Must not be a mutated object from existing data.
	 * @return T which previously existed under the given key, if any; null otherwise
	 */
	public abstract <T> @Nullable T setBranchData(JigsawDataType<T> key, @Nullable T data);
	
	/**
	 * Removes data from the branch data map.
	 * @param <T> Type of data to remove
	 * @param key JigsawDataType to remove
	 * @return T which previously existed under the given key, if any; null otherwise
	 */
	public default <T> @Nullable T removeBranchData(JigsawDataType<T> key)
	{
		return this.setBranchData(key, null);
	}
}
