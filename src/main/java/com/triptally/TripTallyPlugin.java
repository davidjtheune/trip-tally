package com.triptally;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@Slf4j
@PluginDescriptor(
	name = "Trip Tally",
	description = "Value highlights, and live profit/loss for a trip including runes, ammo and blowpipe supplies",
	tags = {"profit", "loss", "supplies", "value", "highlight", "blowpipe", "gp"}
)
public class TripTallyPlugin extends Plugin
{
	@Inject
	private TripTallyConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ValueHighlightOverlay highlightOverlay;

	@Inject
	private TripOverlay tripOverlay;

	@Inject
	private CarriedItems carriedItems;

	@Inject
	private ItemPricer pricer;

	@Getter
	@Inject
	private BlowpipeTracker blowpipe;

	@Getter
	private TripMath.Result result;

	@Getter
	private Instant tripStart;

	@Getter
	private boolean bankOpen;

	private Map<Integer, Long> startItems;
	private Set<String> ignoredNames = new HashSet<>();
	// Set from the AWT thread by the hotkey, consumed on the client thread
	private volatile boolean resetRequested = true;

	private final HotkeyListener resetHotkey = new HotkeyListener(() -> config.resetHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			requestReset();
		}
	};

	@Override
	protected void startUp()
	{
		overlayManager.add(highlightOverlay);
		overlayManager.add(tripOverlay);
		keyManager.registerKeyListener(resetHotkey);
		ignoredNames = parseIgnored(config.ignoredItems());
		blowpipe.loadDartType();
		requestReset();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(highlightOverlay);
		overlayManager.remove(tripOverlay);
		keyManager.unregisterKeyListener(resetHotkey);
		startItems = null;
		result = null;
		tripStart = null;
		bankOpen = false;
	}

	void requestReset()
	{
		resetRequested = true;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		blowpipe.onGameTick();

		if (bankOpen)
		{
			return;
		}

		Map<Integer, Long> now = carriedItems.read();
		if (now == null)
		{
			return;
		}

		if (resetRequested || startItems == null)
		{
			resetRequested = false;
			startItems = now;
			tripStart = Instant.now();
			blowpipe.reset();
		}

		PriceSource source = config.tripPriceSource();
		result = TripMath.compute(startItems, now, blowpipe.used(),
			id -> pricer.price(id, source), this::isIgnored);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			startItems = null;
			result = null;
			tripStart = null;
			bankOpen = false;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		blowpipe.loadDartType();
		requestReset();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = false;
			if (config.resetOnBank())
			{
				requestReset();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		blowpipe.onChatMessage(event.getMessage());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!TripTallyConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		ignoredNames = parseIgnored(config.ignoredItems());
		highlightOverlay.invalidate();
	}

	private boolean isIgnored(int itemId)
	{
		return !ignoredNames.isEmpty()
			&& ignoredNames.contains(itemManager.getItemComposition(itemId).getName().toLowerCase(Locale.ROOT));
	}

	private static Set<String> parseIgnored(String list)
	{
		Set<String> names = new HashSet<>();
		for (String name : Splitter.on(',').trimResults().omitEmptyStrings().split(list))
		{
			names.add(name.toLowerCase(Locale.ROOT));
		}
		return names;
	}

	@Provides
	TripTallyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TripTallyConfig.class);
	}
}
