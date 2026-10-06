package com.triptally;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Estimates blowpipe darts and scales used. The game doesn't expose the blowpipe's contents
 * to the client, so each shot adds its expected loss (Ava's device aware), and the dart type
 * is learned from the blowpipe's "Check" message.
 */
@Slf4j
@Singleton
class BlowpipeTracker
{
	private static final String DART_KEY = "blowpipeDartId";
	private static final Pattern CHECK_MESSAGE = Pattern.compile(
		"Darts: <col=007f00>(.+?) dart x ([\\d,]+)</col>\\. Scales: <col=007f00>([\\d,]+) \\(.*");
	private static final Map<String, Integer> DARTS = ImmutableMap.<String, Integer>builder()
		.put("bronze", ItemID.BRONZE_DART)
		.put("iron", ItemID.IRON_DART)
		.put("steel", ItemID.STEEL_DART)
		.put("black", ItemID.BLACK_DART)
		.put("mithril", ItemID.MITHRIL_DART)
		.put("adamant", ItemID.ADAMANT_DART)
		.put("rune", ItemID.RUNE_DART)
		.put("amethyst", ItemID.AMETHYST_DART)
		.put("dragon", ItemID.DRAGON_DART)
		.build();

	/** Zulrah's scales used per shot. */
	private static final double SCALES_PER_SHOT = 2.0 / 3.0;
	private static final double LOSS_ASSEMBLER = 0.20;
	private static final double LOSS_ACCUMULATOR = 0.28;
	private static final double LOSS_ATTRACTOR = 0.40;
	private static final double LOSS_NONE = 1.0;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private TripTallyConfig config;

	/** Item id of the loaded darts, or -1 until the blowpipe has been checked. */
	@Getter
	private int dartId = -1;
	private double dartsUsed;
	private double scalesUsed;

	void loadDartType()
	{
		Integer saved = configManager.getRSProfileConfiguration(TripTallyConfig.GROUP, DART_KEY, Integer.class);
		dartId = saved != null ? saved : -1;
	}

	void reset()
	{
		dartsUsed = 0;
		scalesUsed = 0;
	}

	/** True when shots were counted but the dart type is still unknown. */
	boolean needsCheck()
	{
		return dartId == -1 && dartsUsed > 0;
	}

	/** Estimated supplies used this trip, item id -> quantity. */
	Map<Integer, Double> used()
	{
		Map<Integer, Double> used = new HashMap<>();
		if (dartId != -1 && dartsUsed > 0)
		{
			used.put(dartId, dartsUsed);
		}
		if (scalesUsed > 0)
		{
			used.put(ItemID.SNAKEBOSS_SCALE, scalesUsed);
		}
		return used;
	}

	void onGameTick()
	{
		Player player = client.getLocalPlayer();
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (player == null || worn == null)
		{
			return;
		}

		Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null || !isLoadedBlowpipe(weapon.getId()))
		{
			return;
		}

		if (player.getAnimation() == AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK && player.getAnimationFrame() == 0)
		{
			dartsUsed += dartLossChance(worn);
			scalesUsed += SCALES_PER_SHOT;
			log.debug("Blowpipe shot: darts {}, scales {}, scale varbit {}",
				dartsUsed, scalesUsed, client.getVarbitValue(VarbitID.CHARGES_TOXIC_BLOWPIPE_QUANTITY));
		}
	}

	void onChatMessage(String message)
	{
		Matcher m = CHECK_MESSAGE.matcher(message);
		if (!m.matches())
		{
			return;
		}

		String dart = m.group(1).toLowerCase(Locale.ROOT);
		Integer id = DARTS.get(dart);
		log.debug("Blowpipe check: {} darts x {}, scales {} (scale varbit {})",
			dart, m.group(2), m.group(3), client.getVarbitValue(VarbitID.CHARGES_TOXIC_BLOWPIPE_QUANTITY));
		if (id != null && id != dartId)
		{
			dartId = id;
			configManager.setRSProfileConfiguration(TripTallyConfig.GROUP, DART_KEY, id);
		}
	}

	private double dartLossChance(ItemContainer worn)
	{
		Item cape = worn.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
		if (cape == null)
		{
			return LOSS_NONE;
		}

		// Name match covers ornament/trouver/broken variants and max capes
		String name = itemManager.getItemComposition(cape.getId()).getName().toLowerCase(Locale.ROOT);
		if (name.contains("assembler") || name.contains("dizana"))
		{
			return LOSS_ASSEMBLER;
		}
		if (name.contains("ranging cape"))
		{
			return config.vorkathHead() ? LOSS_ASSEMBLER : LOSS_ACCUMULATOR;
		}
		if (name.contains("accumulator") || name.equals("max cape"))
		{
			return LOSS_ACCUMULATOR;
		}
		if (name.contains("attractor"))
		{
			return LOSS_ATTRACTOR;
		}
		return LOSS_NONE;
	}

	private static boolean isLoadedBlowpipe(int itemId)
	{
		return itemId == ItemID.TOXIC_BLOWPIPE_LOADED || itemId == ItemID.TOXIC_BLOWPIPE_LOADED_ORNAMENT;
	}
}
