package net.commoble.structurebuddy.api;

import org.jetbrains.annotations.Nullable;

/**
 * Readonly interface for getting shared jigsaw data during element baking.
 */
public interface JigsawDataReader
{
	/**
	 * Retrieves data added to the global data storage by any piece in this structure previously baked and selected for placement.
	 * @param <T> Type of data to retrieve
	 * @param key JigsawDataType for the data being retrieved
	 * @return Data stored under the given key, or null if no such data. Returned data must not be modified.
	 */
	public <T> @Nullable T getGlobalData(JigsawDataType<T> key);
	
	/**
	 * Retrieves data added to the branch data storage by any parent piece of the piece currently being baked.
	 * @param <T> Type of data to retrieve
	 * @param key JigsawDataType for the data being retrieved
	 * @return Data stored under the given key, or null if no such data. Returned data must not be modified.
	 */
	public <T> @Nullable T getBranchData(JigsawDataType<T> key);
	
	/**
	 * Retrieves data added to either the global or branch data storage, preferring the branch data if available.
	 * @param <T> Type of data to retrieve
	 * @param key JigsawDataType for the data being retrieved
	 * @return Data stored in the branch data by a parent piece if present,
	 * or data stored in global storage by any piece previously placed in this structure otherwise,
	 * or null if no such data there either. Returned data must not be modified.
	 */
	public default <T> @Nullable T getData(JigsawDataType<T> key)
	{
		T branchData = this.getBranchData(key);
		return branchData == null ? this.getGlobalData(key) : branchData;
	}
}
