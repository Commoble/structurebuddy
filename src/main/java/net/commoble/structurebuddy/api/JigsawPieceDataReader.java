package net.commoble.structurebuddy.api;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Interface for retreiving jigsaw branch data during piece filling.
 */
public interface JigsawPieceDataReader
{
	/**
	 * Retrieves jigsaw data which existed at the time after some jigsaw piece was baked and selected.
	 * @param <T> Type of data to retrieve
	 * @param key JigsawDataType for the data being retrieved
	 * @return Data which existed in branch storage after the piece was selected, if available,
	 * or otherwise data from global storage's state at that time, if available,
	 * or otherwise null if no such data there either.
	 * Returned data must not be modified.
	 */
	public abstract <T> @Nullable T getData(JigsawDataType<T> key);
	
	/// {@return Map of all key-value entries in this reader. Map and its contents are non-null and immutable}
	public abstract Map<JigsawDataType<?>, Object> toMap();
}
