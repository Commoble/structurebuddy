package net.commoble.structurebuddy.api.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

/** Extra RNG utils **/
public final class RandomBuddy
{
	private RandomBuddy() {} // util class
	
	/**
	 * Shuffles a WeightedList such that elements with higher weights are more likely to be in front 
	 * @param <T> type of things in the list
	 * @param weightedList WeightedList
	 * @param random RandomSource
	 * @return List of things in the list shuffled respecting weight
	 */
	public static <T> List<T> shuffleWeightedList(WeightedList<T> weightedList, RandomSource random)
	{
		List<T> results = new ArrayList<>();
		
		WeightedList<T> remainingWeightedList = weightedList;
		while (remainingWeightedList.unwrap().size() > 0)
		{
			T selected = remainingWeightedList.getRandomOrThrow(random);
			results.add(selected);
			List<Weighted<T>> unselected = new ArrayList<>();
			for (var weighted : remainingWeightedList.unwrap())
			{
				if (weighted.value() != selected)
				{
					unselected.add(weighted);
				}
			}
			remainingWeightedList = WeightedList.of(unselected);
		}
		
		return results;
	}
}
