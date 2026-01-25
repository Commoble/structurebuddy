package net.commoble.structurebuddy.internal;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.commoble.structurebuddy.api.JigsawDataAccess;
import net.minecraft.util.context.ContextKey;

/**
 * Impl of JigsawDataAccess holding mutable data
 * @param globalData Map of data shared by an entire dynamic jigsaw structure
 * @param branchData Map of data which a given piece will make available only to its descendent pieces
 */
public record JigsawData(Map<ContextKey<?>,Object> globalData, Map<ContextKey<?>,Object> branchData) implements JigsawDataAccess
{

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getGlobalData(ContextKey<T> key)
	{
		return (@Nullable T) this.globalData.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getBranchData(ContextKey<T> key)
	{
		return (@Nullable T) this.branchData.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T setGlobalData(ContextKey<T> key, T data)
	{
		return (@Nullable T) this.globalData.put(key, data);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T setBranchData(ContextKey<T> key, T data)
	{
		return (@Nullable T) this.branchData.put(key, data);
	}
	
	/**
	 * {@return JigsawData containing a shallow copy of the branch data (and sharing a reference to the global data)}
	 */
	public JigsawData fork()
	{
		return new JigsawData(this.globalData, new HashMap<>(this.branchData));
	}
}