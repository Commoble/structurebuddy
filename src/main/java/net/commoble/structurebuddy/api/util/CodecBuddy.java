package net.commoble.structurebuddy.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

/**
 * Extra codecs used by StructureBuddy which may be useful
 */
public final class CodecBuddy
{
	private CodecBuddy() {}
	
	/**
	 * Creates a codec which encodes a Map as a list of pairs, averting unboundedMap's requirement that key codecs are string-serializable.
	 * Maps/lists created are mutable.
	 * @param <K> Type of the map keys
	 * @param <V> Type of the map values
	 * @param keyCodec Key serializer
	 * @param valueCodec Value serializer
	 * @return Codec which encodes a Map as a list of pairs
	 */
	public static <K,V> Codec<Map<K,V>> pairListMap(Codec<K> keyCodec, Codec<V> valueCodec)
	{
		return Codec.mapPair(keyCodec.fieldOf("key"), valueCodec.fieldOf("value"))
			.codec()
			.listOf()
			.xmap(list -> {
				Map<K,V> map = new HashMap<>();
				for (var entry : list)
				{
					map.put(entry.getFirst(), entry.getSecond());
				}
				return map;
			}, map -> {
				List<Pair<K,V>> list = new ArrayList<>();
				for (var entry : map.entrySet())
				{
					list.add(Pair.of(entry.getKey(), entry.getValue()));
				}
				return list;
			});
	}

	/**
	 * Helper to make a dispatch codec for a custom forge registry of codecs.
	 * @param <T> Type of the element to be loaded from json
	 * @param registryKey ResourceKey for a Registry of sub-codecs.
	 * @param typeCodec Function to retrieve the codec for a given json-loaded element
	 * @return dispatch codec to load instances of T
	 */
	@SuppressWarnings("unchecked")
	public static <T> Codec<T> dispatch(ResourceKey<Registry<MapCodec<? extends T>>> registryKey, Function<? super T, ? extends MapCodec<? extends T>> typeCodec)
	{
		return Codec.lazyInitialized(() -> {
			// eclipsec and javac need to agree on the generics, so this might look strange
			Registry<?> uncastRegistry = Objects.requireNonNull(BuiltInRegistries.REGISTRY.getValue(registryKey.identifier())); // makes eclipse's nullchecker happy
			Registry<MapCodec<? extends T>> registry = (Registry<MapCodec<? extends T>>) uncastRegistry;
			return registry.byNameCodec().dispatch(typeCodec, Function.identity());
		});
	}
	
	/**
	 * {@return lazy initialized Codec for objects of the given registry (serializes to object identifier}
	 * @param <T> Type of things in the registry
	 * @param registryKey ResourceKey of the registry to make a codec for
	 */
	public static <T> Codec<T> registryCodec(ResourceKey<Registry<T>> registryKey)
	{
		return Codec.lazyInitialized(() -> {
			Registry<?> uncastRegistry = Objects.requireNonNull(BuiltInRegistries.REGISTRY.getValue(registryKey.identifier())); // makes eclipse's nullchecker happy
			@SuppressWarnings("unchecked")
			Registry<T> castRegistry = (Registry<T>)uncastRegistry;
			return castRegistry.byNameCodec();
		});
	}
}
