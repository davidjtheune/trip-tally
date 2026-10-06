package com.triptally;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TripMathTest
{
	private static final int SHARK = 385;
	private static final int DRAGON_DART = 11230;
	private static final int TWISTED_BOW = 20997;
	private static final int BONES = 526;

	private static long price(int id)
	{
		switch (id)
		{
			case SHARK:
				return 800;
			case DRAGON_DART:
				return 2_000;
			case TWISTED_BOW:
				return 3_000_000_000L; // above the old int cap
			default:
				return 0;
		}
	}

	@Test
	public void suppliesAndLoot()
	{
		Map<Integer, Long> start = new HashMap<>();
		start.put(SHARK, 10L);
		Map<Integer, Long> now = new HashMap<>();
		now.put(SHARK, 4L);
		now.put(BONES, 3L);

		TripMath.Result r = TripMath.compute(start, now, new HashMap<>(), TripMathTest::price, id -> false);

		assertEquals(4_800, r.getSupplies());
		assertEquals(0, r.getLoot());
		assertEquals(-4_800, r.net());
		assertEquals(2, r.getDeltas().size());
	}

	@Test
	public void estimatedSuppliesCountAsUsed()
	{
		Map<Integer, Double> estimated = new HashMap<>();
		estimated.put(DRAGON_DART, 2.5);

		TripMath.Result r = TripMath.compute(new HashMap<>(), new HashMap<>(), estimated, TripMathTest::price, id -> false);

		assertEquals(5_000, r.getSupplies());
		assertEquals(-2.5, r.getDeltas().get(0).getQuantity(), 1e-9);
	}

	@Test
	public void valuesAboveIntCapDoNotOverflow()
	{
		Map<Integer, Long> now = new HashMap<>();
		now.put(TWISTED_BOW, 1L);

		TripMath.Result r = TripMath.compute(new HashMap<>(), now, new HashMap<>(), TripMathTest::price, id -> false);

		assertEquals(3_000_000_000L, r.getLoot());
	}

	@Test
	public void ignoredItemsLeftOut()
	{
		Map<Integer, Long> start = new HashMap<>();
		start.put(SHARK, 10L);

		TripMath.Result r = TripMath.compute(start, new HashMap<>(), new HashMap<>(), TripMathTest::price, id -> id == SHARK);

		assertEquals(0, r.getSupplies());
		assertEquals(0, r.getDeltas().size());
	}

	@Test
	public void biggestChangeListedFirst()
	{
		Map<Integer, Long> start = new HashMap<>();
		start.put(SHARK, 1L);
		Map<Integer, Long> now = new HashMap<>();
		now.put(TWISTED_BOW, 1L);

		TripMath.Result r = TripMath.compute(start, now, new HashMap<>(), TripMathTest::price, id -> false);

		assertEquals(TWISTED_BOW, r.getDeltas().get(0).getItemId());
	}

	@Test
	public void tiers()
	{
		assertEquals(-1, TripMath.tier(5, 10, 100, 1_000));
		assertEquals(0, TripMath.tier(10, 10, 100, 1_000));
		assertEquals(2, TripMath.tier(5_000, 10, 100, 1_000));
		assertEquals(0, TripMath.tier(5_000, 10, 0, 0));
	}
}
