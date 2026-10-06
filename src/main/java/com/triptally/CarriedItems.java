package com.triptally;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;

/**
 * Reads everything the player is carrying, including the contents of storage items the
 * inventory/equipment containers don't show (rune pouch, Dizana's quiver).
 * Must be called on the client thread.
 */
@Singleton
class CarriedItems
{
	private static final int[] RUNE_POUCH_TYPES = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] RUNE_POUCH_QUANTITIES = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
	};

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	/**
	 * @return canonical item id -> quantity, or null if the inventory hasn't loaded yet
	 */
	Map<Integer, Long> read()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return null;
		}

		Map<Integer, Long> items = new HashMap<>();
		boolean hasRunePouch = false;
		boolean hasQuiver = false;

		for (Item item : inventory.getItems())
		{
			hasRunePouch |= isRunePouch(item.getId());
			hasQuiver |= isQuiver(item.getId());
			add(items, item.getId(), item.getQuantity());
		}

		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			for (Item item : worn.getItems())
			{
				hasQuiver |= isQuiver(item.getId());
				add(items, item.getId(), item.getQuantity());
			}
		}

		if (hasRunePouch)
		{
			addRunePouch(items);
		}

		if (hasQuiver)
		{
			add(items,
				client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO),
				client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT));
		}

		return items;
	}

	private void addRunePouch(Map<Integer, Long> items)
	{
		EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		for (int i = 0; i < RUNE_POUCH_TYPES.length; i++)
		{
			int rune = client.getVarbitValue(RUNE_POUCH_TYPES[i]);
			int quantity = client.getVarbitValue(RUNE_POUCH_QUANTITIES[i]);
			if (rune != 0)
			{
				add(items, runes.getIntValue(rune), quantity);
			}
		}
	}

	private void add(Map<Integer, Long> items, int itemId, int quantity)
	{
		if (itemId <= 0 || quantity <= 0)
		{
			return;
		}
		items.merge(itemManager.canonicalize(itemId), (long) quantity, Long::sum);
	}

	private static boolean isRunePouch(int itemId)
	{
		return itemId == ItemID.BH_RUNE_POUCH || itemId == ItemID.BH_RUNE_POUCH_TROUVER
			|| itemId == ItemID.DIVINE_RUNE_POUCH || itemId == ItemID.DIVINE_RUNE_POUCH_TROUVER;
	}

	private boolean isQuiver(int itemId)
	{
		// Name match covers every quiver variant, including the Dizana's max cape
		return itemId > 0 && itemManager.getItemComposition(itemId).getName().contains("Dizana");
	}
}
