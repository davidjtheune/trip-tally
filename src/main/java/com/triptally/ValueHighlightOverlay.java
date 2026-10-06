package com.triptally;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ImageUtil;

class ValueHighlightOverlay extends WidgetItemOverlay
{
	/** Alpha used for fills when the configured color is fully opaque. */
	private static final int DEFAULT_FILL_ALPHA = 80;

	private final ItemManager itemManager;
	private final ItemPricer pricer;
	private final TripTallyConfig config;
	private final Cache<Long, BufferedImage> fillCache = CacheBuilder.newBuilder()
		.maximumSize(256)
		.build();

	@Inject
	ValueHighlightOverlay(ItemManager itemManager, ItemPricer pricer, TripTallyConfig config)
	{
		this.itemManager = itemManager;
		this.pricer = pricer;
		this.config = config;
		showOnInventory();
		showOnEquipment();
		showOnBank();
	}

	void invalidate()
	{
		fillCache.invalidateAll();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.highlightEnabled())
		{
			return;
		}
		if (!config.highlightInBank()
			&& WidgetUtil.componentToInterface(widgetItem.getWidget().getId()) == InterfaceID.BANKMAIN)
		{
			return;
		}

		int quantity = widgetItem.getQuantity();
		long value = pricer.price(itemManager.canonicalize(itemId), config.highlightPriceSource());
		if (config.highlightStackValue())
		{
			value *= quantity;
		}

		Color color = colorFor(value);
		if (color == null)
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		HighlightStyle style = config.highlightStyle();
		if (style == HighlightStyle.FILL || style == HighlightStyle.BOTH)
		{
			BufferedImage fill = fill(itemId, quantity, color);
			if (fill != null)
			{
				graphics.drawImage(fill, bounds.x, bounds.y, null);
			}
		}
		if (style == HighlightStyle.OUTLINE || style == HighlightStyle.BOTH)
		{
			graphics.drawImage(itemManager.getItemOutline(itemId, quantity, color), bounds.x, bounds.y, null);
		}
	}

	private Color colorFor(long value)
	{
		switch (TripMath.tier(value, config.lowValue(), config.mediumValue(), config.highValue(), config.insaneValue()))
		{
			case 0:
				return config.lowColor();
			case 1:
				return config.mediumColor();
			case 2:
				return config.highColor();
			case 3:
				return config.insaneColor();
			default:
				return null;
		}
	}

	private BufferedImage fill(int itemId, int quantity, Color color)
	{
		long key = ((long) itemId << 32) ^ ((long) quantity * 31) ^ color.getRGB();
		BufferedImage cached = fillCache.getIfPresent(key);
		if (cached != null)
		{
			return cached;
		}

		BufferedImage image = itemManager.getImage(itemId, quantity, false);
		if (image == null)
		{
			return null;
		}

		Color fillColor = color.getAlpha() == 255
			? new Color(color.getRed(), color.getGreen(), color.getBlue(), DEFAULT_FILL_ALPHA)
			: color;
		BufferedImage filled = ImageUtil.fillImage(image, fillColor);
		fillCache.put(key, filled);
		return filled;
	}
}
