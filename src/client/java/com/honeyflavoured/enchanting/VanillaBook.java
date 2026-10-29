package com.honeyflavoured.enchanting;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the real vanilla enchanting-table book (the animated 3D one).
 *
 * Vanilla keeps that drawing code private inside EnchantmentScreen, so this keeps a hidden vanilla
 * EnchantmentScreen + EnchantmentMenu around, feeds it our centre item, ticks it 20 times a second
 * and calls its private book-drawing method by reflection (found by signature, not by name).
 * If anything about that fails it logs once and the book is simply not drawn.
 */
final class VanillaBook {
	private static final Logger LOGGER = LoggerFactory.getLogger("enchanting");
	private static final long TICK_NS = 50_000_000L;

	/** Set to true to try drawing the vanilla book. false = never touch vanilla's book code (no crashes). */
	private static final boolean ENABLED = true;
	/** true = only print what vanilla's book code looks like to the log, never call it (cannot crash). */
	private static final boolean LOG_ONLY = false;

	private static boolean failed;
	private static Player owner;
	private static EnchantmentMenu menu;
	private static EnchantmentScreen screen;
	private static Method drawMethod;
	private static Method tickMethod;
	private static long lastTick;

	private VanillaBook() {}

	/** x, y = top-left of the GUI. centre = item in our centre slot (empty = book closed). */
	static void draw(GuiGraphicsExtractor graphics, int x, int y, float delta, ItemStack centre) {

		if (!ENABLED || failed) return;

		try {

			Minecraft mc = Minecraft.getInstance();

			if (mc.player == null) return;

			if (screen == null || owner != mc.player) create(mc);

			if (LOG_ONLY) {
				failed = true;   // log once, then stay quiet
				return;
			}

			// Feed the vanilla menu: slot 0 = item (page flip on change), costs[0] != 0 = open.
			if (!ItemStack.matches(menu.getSlot(0).getItem(), centre)) {

				menu.getSlot(0).set(centre.copy());
			}
			menu.costs[0] = centre.isEmpty() ? 0 : 1;

			// Tick the book animation at 20 ticks per second.
			long now = System.nanoTime();
			if (lastTick == 0L) {
				lastTick = now;
			}
			int n = 0;
			while (now - lastTick >= TICK_NS && n < 5) {
				tickMethod.invoke(screen);
				lastTick += TICK_NS;
				n++;
			}
			if (n == 5) {
				lastTick = now;
			}

			Class<?>[] types = drawMethod.getParameterTypes();
			Object[] args = new Object[types.length];
			int ints = 0;
			for (int i = 0; i < types.length; i++) {

				Class<?> t = types[i];

				if (t == GuiGraphicsExtractor.class) {

					args[i] = graphics;
				}
				else if (t == int.class) {

					args[i] = ints++ == 0 ? x : y;
				}
				else if (t == float.class) {

					args[i] = delta;
				}
				else if (t == double.class) {

					args[i] = (double) delta;
				}
				else {

					throw new IllegalStateException("unsupported book method parameter: " + t + " in " + drawMethod);
				}
			}
			drawMethod.invoke(screen, args);
		}
		catch (Throwable t) {

			failed = true;
			LOGGER.error("Could not draw the vanilla enchanting book; it will be hidden.", t);
		}
	}

	private static void create(Minecraft mc) throws Exception {

		owner = mc.player;
		menu = new EnchantmentMenu(0, mc.player.getInventory());
		screen = new EnchantmentScreen(menu, mc.player.getInventory(), Component.empty());

		// A screen that was never opened has no Minecraft / Font yet; the book code may need them.
		for (Field f : Screen.class.getDeclaredFields()) {

			if (Modifier.isStatic(f.getModifiers())) continue;

			if (f.getType() == Minecraft.class) {

				f.setAccessible(true);
				f.set(screen, mc);
			}
			else if (f.getType() == Font.class) {

				f.setAccessible(true);
				f.set(screen, mc.font);
			}
		}

		drawMethod = null;
		tickMethod = null;
		StringBuilder all = new StringBuilder();
		for (Method m : EnchantmentScreen.class.getDeclaredMethods()) {

			all.append("\n  ").append(m);
			Class<?>[] p = m.getParameterTypes();
			String name = m.getName().toLowerCase();

			if (Modifier.isStatic(m.getModifiers()) || !name.contains("book")) continue;

			if (p.length == 0 && m.getReturnType() == void.class && name.contains("tick")) {

				tickMethod = m;
			}
			else if (p.length > 0 && p[0] == GuiGraphicsExtractor.class && drawMethod == null) {

				drawMethod = m;
			}
		}

		if (LOG_ONLY) {
			StringBuilder dbg = new StringBuilder("VANILLA BOOK DEBUG - paste everything between the markers\n=== EnchantmentScreen methods ===");
			dbg.append(all);
			dbg.append("\n=== EnchantmentScreen fields ===");
			for (Field f : EnchantmentScreen.class.getDeclaredFields()) {
				dbg.append("\n  ").append(f);
			}
			dbg.append("\n=== end ===");
			LOGGER.warn(dbg.toString());
		}

		if (drawMethod == null || tickMethod == null) {
			throw new IllegalStateException("book methods not found in EnchantmentScreen (draw=" + drawMethod + ", tick=" + tickMethod + "). All methods:" + all);
		}
		drawMethod.setAccessible(true);
		tickMethod.setAccessible(true);

		// Vanilla builds its 3D book model inside init(). Without this the model is null and
		// the renderer crashes later, so run init() once on the hidden screen.
		Method init = EnchantmentScreen.class.getDeclaredMethod("init");
		init.setAccessible(true);
		init.invoke(screen);
		Field bookModel = EnchantmentScreen.class.getDeclaredField("bookModel");
		bookModel.setAccessible(true);

		if (bookModel.get(screen) == null) {

			throw new IllegalStateException("bookModel is still null after init()");
		}
		lastTick = 0L;
	}
}