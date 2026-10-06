package com.triptally;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

@Singleton
class ItemPricer
{
	@Inject
	private ItemManager itemManager;

	/**
	 * Price of one item. Untradeable items are worth 0 on the Grand Exchange.
	 */
	long price(int itemId, PriceSource source)
	{
		if (itemId == ItemID.COINS)
		{
			return 1;
		}
		if (itemId == ItemID.PLATINUM)
		{
			return 1000;
		}

		switch (source)
		{
			case HIGH_ALCHEMY:
				return highAlch(itemId);
			case HIGHEST:
				return Math.max(itemManager.getItemPrice(itemId), highAlch(itemId));
			case GRAND_EXCHANGE:
			default:
				return itemManager.getItemPrice(itemId);
		}
	}

	private long highAlch(int itemId)
	{
		return itemManager.getItemComposition(itemId).getHaPrice();
	}
}
