package com.triptally;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(TripTallyConfig.GROUP)
public interface TripTallyConfig extends Config
{
	String GROUP = "triptally";

	@ConfigSection(
		name = "Value highlights",
		description = "Highlight items by how much they are worth",
		position = 0
	)
	String highlightSection = "highlightSection";

	@ConfigSection(
		name = "Trip profit/loss",
		description = "Track what a trip costs and what it earns",
		position = 1
	)
	String tripSection = "tripSection";

	// ---- Value highlights ----

	@ConfigItem(
		keyName = "highlightEnabled",
		name = "Enable highlights",
		description = "Highlight items in your inventory and equipment by value",
		section = highlightSection,
		position = 0
	)
	default boolean highlightEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightInBank",
		name = "Highlight in bank",
		description = "Also highlight items inside the bank",
		section = highlightSection,
		position = 1
	)
	default boolean highlightInBank()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightPriceSource",
		name = "Price source",
		description = "Which price to judge an item's value by",
		section = highlightSection,
		position = 2
	)
	default PriceSource highlightPriceSource()
	{
		return PriceSource.GRAND_EXCHANGE;
	}

	@ConfigItem(
		keyName = "highlightStackValue",
		name = "Use stack value",
		description = "Judge stackable items by the whole stack's value instead of one item's",
		section = highlightSection,
		position = 3
	)
	default boolean highlightStackValue()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightStyle",
		name = "Style",
		description = "How highlighted items are drawn",
		section = highlightSection,
		position = 4
	)
	default HighlightStyle highlightStyle()
	{
		return HighlightStyle.OUTLINE;
	}

	@ConfigItem(
		keyName = "lowValue",
		name = "Low value",
		description = "Items worth at least this much get the low-value color (0 disables the tier)",
		section = highlightSection,
		position = 5
	)
	default int lowValue()
	{
		return 20_000;
	}

	@Alpha
	@ConfigItem(
		keyName = "lowColor",
		name = "Low color",
		description = "Color for low-value items",
		section = highlightSection,
		position = 6
	)
	default Color lowColor()
	{
		return new Color(0x66B2FF);
	}

	@ConfigItem(
		keyName = "mediumValue",
		name = "Medium value",
		description = "Items worth at least this much get the medium-value color (0 disables the tier)",
		section = highlightSection,
		position = 7
	)
	default int mediumValue()
	{
		return 100_000;
	}

	@Alpha
	@ConfigItem(
		keyName = "mediumColor",
		name = "Medium color",
		description = "Color for medium-value items",
		section = highlightSection,
		position = 8
	)
	default Color mediumColor()
	{
		return new Color(0x99FF99);
	}

	@ConfigItem(
		keyName = "highValue",
		name = "High value",
		description = "Items worth at least this much get the high-value color (0 disables the tier)",
		section = highlightSection,
		position = 9
	)
	default int highValue()
	{
		return 1_000_000;
	}

	@Alpha
	@ConfigItem(
		keyName = "highColor",
		name = "High color",
		description = "Color for high-value items",
		section = highlightSection,
		position = 10
	)
	default Color highColor()
	{
		return new Color(0xFF9600);
	}

	@ConfigItem(
		keyName = "insaneValue",
		name = "Insane value",
		description = "Items worth at least this much get the insane-value color (0 disables the tier)",
		section = highlightSection,
		position = 11
	)
	default int insaneValue()
	{
		return 10_000_000;
	}

	@Alpha
	@ConfigItem(
		keyName = "insaneColor",
		name = "Insane color",
		description = "Color for insane-value items",
		section = highlightSection,
		position = 12
	)
	default Color insaneColor()
	{
		return new Color(0xFF66B2);
	}

	// ---- Trip profit/loss ----

	@ConfigItem(
		keyName = "tripOverlay",
		name = "Show trip overlay",
		description = "Show supplies used, loot gained and net profit for the current trip",
		section = tripSection,
		position = 0
	)
	default boolean tripOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tripPriceSource",
		name = "Price source",
		description = "Which price to value supplies and loot by",
		section = tripSection,
		position = 1
	)
	default PriceSource tripPriceSource()
	{
		return PriceSource.GRAND_EXCHANGE;
	}

	@ConfigItem(
		keyName = "resetOnBank",
		name = "Reset after banking",
		description = "Start a new trip when you close the bank",
		section = tripSection,
		position = 2
	)
	default boolean resetOnBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetHotkey",
		name = "Reset hotkey",
		description = "Start a new trip from what you are carrying now",
		section = tripSection,
		position = 3
	)
	default Keybind resetHotkey()
	{
		return Keybind.NOT_SET;
	}

	@Range(max = 20)
	@ConfigItem(
		keyName = "breakdownLines",
		name = "Item breakdown lines",
		description = "How many of the biggest item changes to list under the totals (0 hides the list)",
		section = tripSection,
		position = 4
	)
	default int breakdownLines()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "ignoredItems",
		name = "Ignored items",
		description = "Comma-separated item names to leave out of the trip totals",
		section = tripSection,
		position = 5
	)
	default String ignoredItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "vorkathHead",
		name = "Ranging cape has Vorkath's head",
		description = "Your ranging cape gives the Ava's assembler effect, for blowpipe dart estimates",
		section = tripSection,
		position = 6
	)
	default boolean vorkathHead()
	{
		return false;
	}
}
