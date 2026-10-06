package com.triptally;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

class TripOverlay extends OverlayPanel
{
	private static final Color GAIN = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color LOSS = ColorScheme.PROGRESS_ERROR_COLOR;

	private final TripTallyPlugin plugin;
	private final TripTallyConfig config;
	private final ItemManager itemManager;

	@Inject
	TripOverlay(TripTallyPlugin plugin, TripTallyConfig config, ItemManager itemManager)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_LEFT);
		addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Reset", "Trip Tally", e -> plugin.requestReset());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		TripMath.Result result = plugin.getResult();
		Instant start = plugin.getTripStart();
		if (!config.tripOverlay() || result == null || start == null)
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("Trip Tally").build());

		if (plugin.isBankOpen())
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Banking...").build());
			return super.render(graphics);
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Time")
			.right(formatDuration(Duration.between(start, Instant.now())))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Supplies")
			.right(format(-result.getSupplies()))
			.rightColor(result.getSupplies() > 0 ? LOSS : Color.WHITE)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Loot")
			.right(format(result.getLoot()))
			.rightColor(result.getLoot() > 0 ? GAIN : Color.WHITE)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Net")
			.right(format(result.net()))
			.rightColor(colorFor(result.net()))
			.build());

		if (plugin.getBlowpipe().needsCheck())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Check blowpipe for dart type")
				.leftColor(Color.YELLOW)
				.build());
		}

		int lines = Math.min(config.breakdownLines(), result.getDeltas().size());
		for (int i = 0; i < lines; i++)
		{
			TripMath.ItemDelta d = result.getDeltas().get(i);
			String name = itemManager.getItemComposition(d.getItemId()).getName();
			panelComponent.getChildren().add(LineComponent.builder()
				.left(name + " " + formatQuantity(d.getQuantity()))
				.right(format(d.getValue()))
				.rightColor(colorFor(d.getValue()))
				.build());
		}

		return super.render(graphics);
	}

	private static Color colorFor(long value)
	{
		return value > 0 ? GAIN : value < 0 ? LOSS : Color.WHITE;
	}

	private static String format(long value)
	{
		String sign = value > 0 ? "+" : value < 0 ? "-" : "";
		return sign + QuantityFormatter.quantityToStackSize(Math.abs(value));
	}

	private static String formatQuantity(double qty)
	{
		long rounded = Math.round(qty);
		if (Math.abs(qty - rounded) < 1e-9)
		{
			return (rounded > 0 ? "+" : "") + rounded;
		}
		return String.format("%+.1f", qty);
	}

	private static String formatDuration(Duration d)
	{
		long s = d.getSeconds();
		return s >= 3600
			? String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
			: String.format("%d:%02d", s / 60, s % 60);
	}
}
