package net.commoble.structurebuddy.internal;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.commoble.structurebuddy.api.JigsawDataAccess;
import net.commoble.structurebuddy.api.JigsawDataType;

/**
 * Impl of JigsawDataAccess holding mutable data
 * @param globalData Map of data shared by an entire dynamic jigsaw structure
 * @param branchData Map of data which a given piece will make available only to its descendent pieces
 */
public record JigsawData(Map<JigsawDataType<?>,Object> globalData, Map<JigsawDataType<?>,Object> branchData) implements JigsawDataAccess
{

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getGlobalData(JigsawDataType<T> key)
	{
		return (@Nullable T) this.globalData.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getBranchData(JigsawDataType<T> key)
	{
		return (@Nullable T) this.branchData.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T setGlobalData(JigsawDataType<T> key, @Nullable T data)
	{
		return data == null
			? (T)this.globalData.remove(key)
			: (@Nullable T) this.globalData.put(key, data);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T setBranchData(JigsawDataType<T> key, @Nullable T data)
	{
		return data == null
			? (T)this.branchData.remove(key)
			: (@Nullable T) this.branchData.put(key, data);
	}
	
	/**
	 * {@return JigsawData containing a shallow copy of the branch data (and sharing a reference to the global data)}
	 */
	@Override
	public JigsawData fork()
	{
		return new JigsawData(this.globalData, new HashMap<>(this.branchData));
	}
	
	/**
	 * {@return Map containing entries from both global and branch data at this time,
	 * using branch data for JigsawDataTypes which exist in both data sets}
	 */
	public Map<JigsawDataType<?>, Object> toMap()
	{
		Map<JigsawDataType<?>, Object> map = new HashMap<>();
		for (var entry : this.globalData.entrySet())
		{
			map.put(entry.getKey(), entry.getValue());
		}
		for (var entry : this.branchData.entrySet())
		{
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}
}