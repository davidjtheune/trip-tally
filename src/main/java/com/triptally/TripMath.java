package com.triptally;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntToLongFunction;
import lombok.Value;

/**
 * Pure profit/loss arithmetic, kept free of client classes so it can be unit tested.
 * All gp values are longs: since the 2026 GE update, prices can exceed Integer.MAX_VALUE.
 */
final class TripMath
{
	private TripMath()
	{
	}

	@Value
	static class ItemDelta
	{
		int itemId;
		/** Positive = gained, negative = used. Fractional for estimated supplies like blowpipe darts. */
		double quantity;
		long value;
	}

	@Value
	static class Result
	{
		/** Total value of everything used, as a positive number. */
		long supplies;
		/** Total value of everything gained. */
		long loot;
		/** Every change, biggest absolute value first. */
		List<ItemDelta> deltas;

		long net()
		{
			return loot - supplies;
		}
	}

	/**
	 * @param start     item id -> quantity carried when the trip started
	 * @param now       item id -> quantity carried now
	 * @param estimated item id -> quantity used that the containers can't show (positive = used)
	 * @param price     price of one item
	 * @param ignored   items to leave out entirely
	 */
	static Result compute(Map<Integer, Long> start, Map<Integer, Long> now, Map<Integer, Double> estimated,
		IntToLongFunction price, IntPredicate ignored)
	{
		Map<Integer, Double> change = new HashMap<>();
		Set<Integer> ids = new HashSet<>(start.keySet());
		ids.addAll(now.keySet());
		for (int id : ids)
		{
			long diff = now.getOrDefault(id, 0L) - start.getOrDefault(id, 0L);
			if (diff != 0)
			{
				change.put(id, (double) diff);
			}
		}
		for (Map.Entry<Integer, Double> e : estimated.entrySet())
		{
			change.merge(e.getKey(), -e.getValue(), Double::sum);
		}

		long supplies = 0;
		long loot = 0;
		List<ItemDelta> deltas = new ArrayList<>();
		for (Map.Entry<Integer, Double> e : change.entrySet())
		{
			int id = e.getKey();
			double qty = e.getValue();
			if (qty == 0 || ignored.test(id))
			{
				continue;
			}

			long value = Math.round(qty * price.applyAsLong(id));
			if (value < 0)
			{
				supplies -= value;
			}
			else
			{
				loot += value;
			}
			deltas.add(new ItemDelta(id, qty, value));
		}

		deltas.sort(Comparator.comparingLong((ItemDelta d) -> Math.abs(d.getValue())).reversed());
		return new Result(supplies, loot, Collections.unmodifiableList(deltas));
	}

	/**
	 * Picks the highlight tier for a value: index into thresholds of the highest one reached, or -1.
	 * A threshold of 0 or less disables that tier.
	 */
	static int tier(long value, long... thresholds)
	{
		int tier = -1;
		for (int i = 0; i < thresholds.length; i++)
		{
			if (thresholds[i] > 0 && value >= thresholds[i])
			{
				tier = i;
			}
		}
		return tier;
	}
}
