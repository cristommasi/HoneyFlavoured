package com.honeyflavoured.enchanting;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import com.honeyflavoured.HoneyFlavoured;
import com.honeyflavoured.enchanting.EnchantingScreen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;


public class EnchantingScreen extends AbstractContainerScreen<EnchantingMenu> {

    // ------------------------------------------------------------------ textures
    private static final String DIR = "textures/gui/enchanting/container/";
    private static final Identifier BACKGROUND = tex("enchanting_table.png");
    private static final Identifier SLOT_READY = tex("enchantment_slot.png");
    private static final Identifier SLOT_HOVER = tex("enchantment_slot_highlighted.png");
    private static final Identifier ORB = tex("xp_orb.png");
    private static final Identifier ORB_DISABLED = tex("xp_orb_disabled.png");
    private static final int COST_GREEN = 0xFF80FF20;
    private static final int COST_RED = 0xFFFF6060;

    // ------------------------------------------------------------------ layout 
    /* Canvas size of enchanting_table.png / enchantment_slot.png / enchantment_slot_highlighted.png. */
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    /* GUI size inside that canvas (top-left corner). */
    private static final int GUI_W = 176;
    private static final int GUI_H = 166;
    /* Result slot item position and size. */
    private static final int RESULT_X = EnchantingMenu.RESULT_X;
    private static final int RESULT_Y = EnchantingMenu.RESULT_Y;
    private static final int RESULT_SIZE = 16;
    /* Orb + number sit centred under the result slot. Gap between orb and number, and distance below the slot. */
    private static final int COST_GAP = 2;
    private static final int COST_BELOW_SLOT = 3;
    /* Knowledge-book button position, under the vanilla book (relative to the GUI top-left). Size is 20x18. */
    private static final int BTN_X = 24;
    private static final int BTN_Y = 51;
    private static final int BTN_W = 20;
    private static final int BTN_H = 18;
     /* Recipes Panel */
    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int GRID_X = 11;    // first recipe cell, relative to the panel
    private static final int GRID_Y = 31;
    private static final int CELL = 25;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int PER_PAGE = COLS * ROWS;
    /* Search box and "show craftable" toggle, relative to the panel (vanilla positions). */
    private static final int SEARCH_X = 25;
    private static final int SEARCH_Y = 13;
    private static final int SEARCH_W = 81;
    private static final int SEARCH_H = 14;
    private static final int FILTER_X = 110;
    private static final int FILTER_Y = 12;
    private static final int FILTER_W = 26;
    private static final int FILTER_H = 16;
    /* Slot index of the centre (book) slot in the menu;it never counts as an ingredient. */
    private static final int CENTRE_MENU_SLOT = 5;
    /* Red filter over missing ingredients: red at ~19% on the grey slot gives vanilla's #A17171. */
    private static final int GHOST_RED = 0x30FF0000;
    /* The result slot's frame is bigger than the 16x16 item: 26x26, starting 5px up/left of the item. */
    private static final int RESULT_BOX = 26;
    private static final int RESULT_BOX_PAD = 5;
    /* Grey painted over the 14x14 inside of a slot (hint icon in the texture) once it holds an item. */
    private static final int SLOT_COVER = 0xFF8B8B8B;

    // ------------------------------------------------------------------ knowledge book
    // These are vanilla's own textures, so the panel looks exactly like the crafting table's recipe book.
    private static final Identifier V_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final Identifier V_BUTTON = vanillaSprite("button");
    private static final Identifier V_BUTTON_HI = vanillaSprite("button_highlighted");
    private static final Identifier V_TAB_SELECTED = vanillaSprite("tab_selected");
    private static final Identifier V_SLOT = vanillaSprite("slot_craftable");
    private static final Identifier V_SLOT_RED = vanillaSprite("slot_uncraftable");
    private static final Identifier V_PREV = vanillaSprite("page_backward");
    private static final Identifier V_PREV_HI = vanillaSprite("page_backward_highlighted");
    private static final Identifier V_NEXT = vanillaSprite("page_forward");
    private static final Identifier V_NEXT_HI = vanillaSprite("page_forward_highlighted");
    private static final Identifier V_FILTER_ON = vanillaSprite("filter_enabled");
    private static final Identifier V_FILTER_ON_HI = vanillaSprite("filter_enabled_highlighted");
    private static final Identifier V_FILTER_OFF = vanillaSprite("filter_disabled");
    private static final Identifier V_FILTER_OFF_HI = vanillaSprite("filter_disabled_highlighted");






    private boolean panelOpen;
    private int page;
    private int selected = -1;
    private String lastQuery = "";
    private EditBox searchBox;
    private String[] names;
    private ItemStack[] icons;        // enchanted books retained for tooltips and ghost recipes
    private Identifier[] enchantmentTextures; // custom PNGs for the knowledge-book grid
    private ItemStack[] ingredients;  // the recipe's ingredient item
    private ItemStack lapisIcon;
    private ItemStack tabIcon;

    /* Enchantment title and descriptions */
    private static final Map<String, String> DESC = Map.ofEntries(
        Map.entry("mending", "Repairs the item when gaining XP orbs."),
        Map.entry("unbreaking", "Increases item durability."),
        Map.entry("vanishing_curse", "Item destroyed upon death."),
        Map.entry("aqua_affinity", "Increases underwater mining speed."),
        Map.entry("blast_protection", "Reduces explosion damage and knockback."),
        Map.entry("binding_curse", "Item cannot be removed from armor slots."),
        Map.entry("depth_strider", "Increases underwater movement speed."),
        Map.entry("feather_falling", "Reduces fall damage."),
        Map.entry("fire_protection", "Reduces fire damage and burn time."),
        Map.entry("frost_walker", "Turns water beneath you into frosted ice."),
        Map.entry("projectile_protection", "Reduces projectile damage."),
        Map.entry("protection", "Reduces most types of damage."),
        Map.entry("respiration", "Extends underwater breathing time."),
        Map.entry("soul_speed", "Increases speed on soul sand and soul soil."),
        Map.entry("thorns", "Reflects some of the damage taken when hit."),
        Map.entry("swift_sneak", "Increases speed while crouching."),
        Map.entry("bane_of_arthropods", "Increases damage to arthropod mobs."),
        Map.entry("breach", "Reduces the effectiveness of enemy armor."),
        Map.entry("density", "Increases mace damage while falling."),
        Map.entry("efficiency", "Increases mining speed."),
        Map.entry("fire_aspect", "Sets target on fire."),
        Map.entry("looting", "Increases loot dropped by mobs."),
        Map.entry("lunge", "Spear jabs get a burst of speed."),
        Map.entry("impaling", "Trident deals extra damage to ocean mobs."),
        Map.entry("knockback", "Knocks mobs away when hit."),
        Map.entry("sharpness", "Increases weapon damage."),
        Map.entry("smite", "Increases damage to undead mobs."),
        Map.entry("sweeping_edge", "Increases sweeping attack damage."),
        Map.entry("wind_burst", "Bounces you into the air after a hit."),
        Map.entry("channeling", "Trident channels lightning at a hit target."),
        Map.entry("flame", "Arrows set targets on fire."),
        Map.entry("infinity", "Shooting does not consume arrows."),
        Map.entry("loyalty", "Trident returns after being thrown."),
        Map.entry("riptide", "Trident launches you with it in water or rain."),
        Map.entry("multishot", "Shoots 3 arrows at the cost of one."),
        Map.entry("piercing", "Arrows pass through multiple entities."),
        Map.entry("power", "Increases arrow damage."),
        Map.entry("punch", "Increases arrow knockback."),
        Map.entry("quick_charge", "Decreases crossbow charging time."),
        Map.entry("fortune", "Increases item drops from blocks."),
        Map.entry("luck_of_the_sea", "Increases rare loot from fishing."),
        Map.entry("lure", "Decreases wait time until a fish bites."),
        Map.entry("silk_touch", "Mined blocks drop as themselves.")
    );

    /* Fake stack used only for the tooltip: enchant name as title, description underneath. */
    private ItemStack tooltipStack(int idx) {
        String id = Recipes.ORDERED.get(idx).getValue().identifier().getPath();
        ItemStack s = new ItemStack(Items.PAPER);
        s.set(DataComponents.CUSTOM_NAME,
            Component.literal(this.names[idx]).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.AQUA)));
        s.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(DESC.getOrDefault(id, "")).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.GRAY))
        )));
        return s;
    }

    private static final Map<String, int[]> SIZES = new HashMap<>();
    private static Method itemMethod;
    private static boolean itemSearched;
    private static Method tooltipMethod;
    private static boolean tooltipSearched;



    public EnchantingScreen(EnchantingMenu menu, Inventory inventory, Component title) {

        // imageWidth/imageHeight are final in 26.x and already default to 176x166.
        super(menu, inventory, title);
    }



    private static Identifier vanillaSprite(String name) {

        return Identifier.withDefaultNamespace("textures/gui/sprites/recipe_book/" + name + ".png");
    }



    @Override
    protected void init() {

        super.init();

        ClientState.load();

        this.panelOpen = ClientState.panelOpen;   // stays open if you left it open last time

        this.applyShift();

        this.searchBox = new EditBox(this.font, 0, 0, SEARCH_W, SEARCH_H, Component.translatable("itemGroup.search"));

        this.searchBox.setMaxLength(50);

        this.searchBox.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withColor(0xA0A0A0));

        this.searchBox.setTextColor(0xFFFFFFFF);

        this.searchBox.setValue(this.lastQuery);

        this.addRenderableWidget(this.searchBox);

        this.placeSearchBox();
    }



    /* Same as the crafting table: the GUI slides right while the recipe book is open. */

    private void applyShift() {

        this.leftPos = (this.width - this.imageWidth) / 2 + (this.panelOpen ? 77 : 0);
    }



    private void placeSearchBox() {

        if (this.searchBox == null) {
            return;
        }

        this.searchBox.setX(this.panelX() + SEARCH_X);
        this.searchBox.setY(this.topPos + SEARCH_Y);
        this.searchBox.setVisible(this.panelOpen);

        if (!this.panelOpen) {
            this.searchBox.setFocused(false);
        }
    }



    private int panelX() {

        return this.leftPos - PANEL_W - 1;
    }



    private void setPanelOpen(boolean open) {

        this.panelOpen = open;
        ClientState.panelOpen = open;
        ClientState.save();

        if (!open) {
            this.selected = -1;
        }

        this.applyShift();
        this.placeSearchBox();
    }



    private void ensureCache() {

        if (this.names != null) {
            return;
        }

        int n = Recipes.ORDERED.size();
        this.names = new String[n];
        this.icons = new ItemStack[n];
        this.enchantmentTextures = new Identifier[n];
        this.ingredients = new ItemStack[n];
        var lookup = this.menu.registries().lookupOrThrow(Registries.ENCHANTMENT);


        for (int i = 0; i < n; i++) {

            this.names[i] = this.menu.enchantName(i).getString();

            this.ingredients[i] = new ItemStack(Recipes.ORDERED.get(i).getKey());

            String enchantmentId = Recipes.ORDERED.get(i).getValue().identifier().getPath();

            this.enchantmentTextures[i] = Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "textures/gui/enchanting/enchantments/" + enchantmentId + ".png");

            Holder<Enchantment> holder = lookup.getOrThrow(Recipes.ORDERED.get(i).getValue());

            ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

            m.set(holder, 1);

            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

            book.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());

            this.icons[i] = book;

        }
        this.lapisIcon = new ItemStack(Items.LAPIS_LAZULI);
        this.tabIcon = new ItemStack(Items.ENCHANTED_BOOK);
    }


    /* Do you have 5 of the ingredient, 2 flowers and the lapis (inventory + ingredient slots, not the centre slot)? */
    private boolean craftable(int idx) {

        Item ingredient = Recipes.ORDERED.get(idx).getKey();
        List<Item> flowers = Recipes.flowersFor(Recipes.ORDERED.get(idx).getValue());
        int have = 0;
        int lapis = 0;
        int flower = 0;

        for (int i = 1; i < this.menu.slots.size(); i++) {

            if (i == CENTRE_MENU_SLOT) {
                continue;
            }

            ItemStack s = this.menu.slots.get(i).getItem();

            if (s.is(ingredient)) {
                have += s.getCount();
            }

            if (s.is(Items.LAPIS_LAZULI)) {
                lapis += s.getCount();
            }

            if (flowers.contains(s.getItem())) {
                flower += s.getCount();
            }
        }
        return have >= Recipes.RING.length && lapis >= EnchantingMenu.LAPIS_COST && flower >= 2;
    }



    /* Unlocked recipe indices that match the search / craftable filter, sorted by enchantment name. */

    private List<Integer> shown() {

        this.ensureCache();
        String q = this.searchBox == null ? "" : this.searchBox.getValue().toLowerCase();

        if (!q.equals(this.lastQuery)) {
            this.lastQuery = q;
            this.page = 0;
        }

        List<Integer> out = new ArrayList<>();

        for (int i = 0; i < Recipes.ORDERED.size(); i++) {

            if (!this.menu.isUnlocked(i)) {
                continue;
            }

            if (!q.isEmpty() && !this.names[i].toLowerCase().contains(q)) {
                continue;
            }

            if (ClientState.craftableOnlyToggle && !this.craftable(i)) {
                continue;
            }
            out.add(i);
        }
        out.sort(Comparator.comparing(i -> this.names[i]));
        return out;
    }



    /* Draws an item icon. The method is looked up by signature because its name changed across versions. */

    private static void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {

        if (!itemSearched) {

            itemSearched = true;

            for (Method m : GuiGraphicsExtractor.class.getMethods()) {

                Class<?>[] p = m.getParameterTypes();

                if (p.length == 3 && p[0] == ItemStack.class && p[1] == int.class && p[2] == int.class && (itemMethod == null || m.getName().equals("item"))) {

                    itemMethod = m;

                }
            }
        }

        if (itemMethod != null) {

            try {

                itemMethod.invoke(graphics, stack, x, y);

            } catch (ReflectiveOperationException ignored) {

                itemMethod = null;

            }

        }

    }


    /* Vanilla item tooltip (name, enchantments...) next to the mouse. Looked up by signature like drawItem. */
    private void tooltip(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {

        if (!tooltipSearched) {

            tooltipSearched = true;

            for (Method m : GuiGraphicsExtractor.class.getMethods()) {

                Class<?>[] p = m.getParameterTypes();

                if (p.length == 4 && p[0] == Font.class && p[1] == ItemStack.class && p[2] == int.class && p[3] == int.class && m.getName().toLowerCase().contains("tooltip")) {

                    tooltipMethod = m;
                    break;
                }

            }

        }
        if (tooltipMethod != null) {

            try {

                tooltipMethod.invoke(graphics, this.font, stack, x, y);

            } catch (ReflectiveOperationException ignored) {

                tooltipMethod = null;

            }
        }

    }



    private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {

        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F, w, h, w, h);

    }

    /** Draws a custom enchantment PNG at 16x16 pixels in the knowledge-book grid. */
    private static void drawEnchantmentIcon(GuiGraphicsExtractor graphics, Identifier texture, int x, int y) {

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
    }



    private boolean overButton(double mx, double my) {

        return over(mx, my, this.leftPos + BTN_X, this.topPos + BTN_Y, BTN_W, BTN_H);

    }



    /* Which recipe cell (0..PER_PAGE-1) is at this position, or -1. */

    private int cellAt(double mx, double my) {

        double rx = mx - (this.panelX() + GRID_X);
        double ry = my - (this.topPos + GRID_Y);

        if (rx < 0 || ry < 0) {
            return -1;
        }

        int col = (int) (rx / CELL);
        int row = (int) (ry / CELL);

        return (col < COLS && row < ROWS) ? row * COLS + col : -1;
    }



    private static boolean over(double mx, double my, int x, int y, int w, int h) {

        return mx >= x && mx < x + w && my >= y && my < y + h;
    }



    private int pageCount() {

        return Math.max(1, (this.shown().size() + PER_PAGE - 1) / PER_PAGE);
    }


    private boolean searchDragging;

    private boolean shiftDown;

    /* Shift state of a click. GUI screens don't update the sneak key, so read the click's own modifiers. */
    private boolean shiftHeld(MouseButtonEvent event) {
        try {
            Object r = event.getClass().getMethod("hasShiftDown").invoke(event);
            if (r instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // try the next way
        }
        try {
            Object r = event.getClass().getMethod("modifiers").invoke(event);
            if (r instanceof Integer m) {
                return (m & 1) != 0;
            }
        } catch (ReflectiveOperationException ignored) {
            // fall back below
        }
        return this.minecraft != null && this.minecraft.options.keyShift.isDown();
    }

    private boolean isShiftHeld() {
        return this.shiftDown;
    }

    private boolean overSearch(double mx, double my) {
        return this.panelOpen && over(mx, my, this.panelX() + SEARCH_X, this.topPos + SEARCH_Y, SEARCH_W, SEARCH_H);
    }

    /* Takes keyboard focus away from the search box so slots and recipes work normally again. */
    private void unfocusSearch() {
        if (this.searchBox != null) {
            this.searchBox.setFocused(false);
        }
        if (this.getFocused() == this.searchBox) {
            this.setFocused(null);
        }
        this.searchDragging = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        // Escape always closes the entire screen.
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (this.searchBox != null) {
                this.searchBox.setValue("");
                this.searchBox.setFocused(false);
            }
            this.lastQuery = "";
            this.selected = -1;
            this.onClose();
            return true;
        }

        // While typing, the search box gets every key (backspace, delete, select all, E...).
        if (this.panelOpen && this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.keyPressed(event);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        double mx = event.x();
        double my = event.y();
        this.shiftDown = this.shiftHeld(event);

        // Click inside the search box: we handle focus, cursor and drag-selection ourselves.
        if (this.searchBox != null && this.overSearch(mx, my)) {
            this.setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            this.searchBox.mouseClicked(event, doubleClick);
            this.searchDragging = true;
            return true;
        }

        // Any other click leaves the search box, so slots and recipes are never blocked.
        this.unfocusSearch();

        // 0 and 1 both count as a click (this version reports left-click as 1).
        if (event.button() == 0 || event.button() == 1) {

            if (this.overButton(mx, my)) {
                this.setPanelOpen(!this.panelOpen);
                return true;
            }
            if (this.panelOpen && this.panelClick(mx, my)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {

        // Only a drag that started inside the search box selects text; everything else drags items normally.
        if (this.searchDragging && this.searchBox != null) {
            return this.searchBox.mouseDragged(event, dragX, dragY);
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {

        if (this.searchDragging) {
            this.searchDragging = false;
            if (this.searchBox != null) {
                this.searchBox.mouseReleased(event);
            }
            return true;
        }
        return super.mouseReleased(event);
    }


    private boolean panelClick(double mx, double my) {

        int px = this.panelX();
        int py = this.topPos;
        boolean onTab = over(mx, my, px - 32, py + 3, 35, 27);

        if (!onTab && !over(mx, my, px, py, PANEL_W, PANEL_H)) {

            return false;
        }

        // Search box: let the normal widget code focus it and place the cursor.
        if (over(mx, my, px + SEARCH_X, py + SEARCH_Y, SEARCH_W, SEARCH_H)) {

            return false;
        }

        if (this.searchBox != null) {

            this.searchBox.setFocused(false);
        }

        if (over(mx, my, px + FILTER_X, py + FILTER_Y, FILTER_W, FILTER_H)) {

            ClientState.craftableOnlyToggle = !ClientState.craftableOnlyToggle;
            ClientState.save();
            this.page = 0;
            return true;
        }

        int pages = this.pageCount();

        if (over(mx, my, px + 38, py + 137, 12, 17)) {

            this.page = Math.max(0, this.page - 1);
            return true;
        }

        if (over(mx, my, px + 93, py + 137, 12, 17)) {

            this.page = Math.min(pages - 1, this.page + 1);
            return true;
        }

        int cell = this.cellAt(mx, my);
        List<Integer> shown = this.shown();
        int k = this.page * PER_PAGE + cell;

        if (cell >= 0 && k < shown.size() && this.minecraft != null) {

            int idx = shown.get(k);
			this.selected = idx;
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
					idx + (this.isShiftHeld() ? EnchantingMenu.FILL_ALL : 0));
        }
        return true;
    }



    /* Ghost recipe in the grid (red because you lack the items), on the still-empty slots. */

    private void drawGhost(GuiGraphicsExtractor graphics) {

        if (this.selected < 0) {
            return;
        }

        this.ensureCache();
        // Vanilla clears the ghost as soon as you place something yourself.
        boolean empty = true;

        for (int gi : Recipes.RING) {
            empty &= this.menu.slots.get(1 + gi).getItem().isEmpty();
        }
        for (int gi : Recipes.FLOWER_SLOTS) {
            empty &= this.menu.slots.get(1 + gi).getItem().isEmpty();
        }
        if (!empty) {
            return;
        }

        for (int gi : Recipes.RING) {
            this.ghostSlot(graphics, this.menu.slots.get(1 + gi), this.ingredients[this.selected], 0, 16, false);
        }

        // Flower slots: when a recipe accepts several flowers (any tulip...), cycle through them like vanilla does.
        List<Item> flowers = Recipes.flowersFor(Recipes.ORDERED.get(this.selected).getValue());
        if (!flowers.isEmpty()) {
            ItemStack flower = new ItemStack(flowers.get((int) ((System.currentTimeMillis() / 1000) % flowers.size())));
            for (int gi : Recipes.FLOWER_SLOTS) {
                this.ghostSlot(graphics, this.menu.slots.get(1 + gi), flower, 0, 16, true);
            }
        }

        this.ghostSlot(graphics, this.menu.slots.get(1 + 7), this.lapisIcon, 0, 16, true);   // lapis slot (grid index 7)
        if (!this.menu.hasResult()) {

            this.ghostSlot(graphics, this.menu.slots.get(0), this.icons[this.selected], RESULT_BOX_PAD, RESULT_BOX, false);
        }

    }



    private void ghostSlot(GuiGraphicsExtractor graphics, net.minecraft.world.inventory.Slot slot,
        ItemStack stack, int pad, int size, boolean cover) {
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;

        if (cover) {
            graphics.fill(x + 1, y + 1, x + 15, y + 15, SLOT_COVER);
        }
        drawItem(graphics, stack, x, y);
        graphics.fill(x - pad, y - pad, x - pad + size, y - pad + size, GHOST_RED);
    }



    private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {

        this.ensureCache();
        int px = this.panelX();
        int py = this.topPos;



        // Vanilla recipe-book background (includes the magnifying glass and the search field frame).
        graphics.blit(RenderPipelines.GUI_TEXTURED, V_BACKGROUND, px, py, 1.0F, 1.0F, PANEL_W, PANEL_H, 256, 256);
        sprite(graphics, V_TAB_SELECTED, px - 32, py + 3, 35, 27);
        drawItem(graphics, this.tabIcon, px - 32 + 9, py + 3 + 5);



        // "Show only craftable" toggle next to the search box.
        boolean filterHover = over(mouseX, mouseY, px + FILTER_X, py + FILTER_Y, FILTER_W, FILTER_H);

        Identifier filterSprite = ClientState.craftableOnlyToggle ? (filterHover ? V_FILTER_ON_HI : V_FILTER_ON) : (filterHover ? V_FILTER_OFF_HI : V_FILTER_OFF);

        sprite(graphics, filterSprite, px + FILTER_X, py + FILTER_Y, FILTER_W, FILTER_H);

        List<Integer> shown = this.shown();

        int pages = this.pageCount();

        this.page = Math.max(0, Math.min(this.page, pages - 1));



        // Recipe grid: custom enchantment PNG per recipe; red frame = you lack the ingredients.
        int hovered = -1;
        int hoverCell = this.cellAt(mouseX, mouseY);

        for (int c = 0; c < PER_PAGE; c++) {

            int k = this.page * PER_PAGE + c;

            if (k >= shown.size()) {
                break;
            }

            int idx = shown.get(k);
            int cx = px + GRID_X + (c % COLS) * CELL;
            int cy = py + GRID_Y + (c / COLS) * CELL;

            sprite(graphics, this.craftable(idx) ? V_SLOT : V_SLOT_RED, cx, cy, CELL, CELL);
            drawEnchantmentIcon(graphics, this.enchantmentTextures[idx], cx + 4, cy + 4);

            if (c == hoverCell) {

                hovered = idx;
                graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x40FFFFFF);
            }
        }
        if (shown.isEmpty()) {

            graphics.text(this.font, Component.literal("No recipes"), px + 14, py + 40, 0xFF404040, false);
        }



        // Page arrows (only when there is more than one page), like vanilla.
        if (pages > 1) {

            boolean backHi = over(mouseX, mouseY, px + 38, py + 137, 12, 17);
            boolean fwdHi = over(mouseX, mouseY, px + 93, py + 137, 12, 17);

            if (this.page > 0) {

                sprite(graphics, backHi ? V_PREV_HI : V_PREV, px + 38, py + 137, 12, 17);
            }

            if (this.page < pages - 1) {

                sprite(graphics, fwdHi ? V_NEXT_HI : V_NEXT, px + 93, py + 137, 12, 17);
            }

            Component label = Component.literal((this.page + 1) + "/" + pages);
            graphics.text(this.font, label, px + 73 - this.font.width(label) / 2, py + 141, 0xFFFFFFFF, false);
        }
        // Vanilla item tooltip for the hovered recipe.
        if (hovered >= 0) {

            this.tooltip(graphics, this.tooltipStack(hovered), mouseX, mouseY);
        }

    }



    private static Identifier tex(String file) {

        return Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, DIR + file);
    }



    /* Reads a PNG's pixel size from the mod jar so you can use any sprite size. */

    private static int[] sizeOf(String file, int fallbackW, int fallbackH) {

        return SIZES.computeIfAbsent(file, f -> {

            String path = "/assets/" + HoneyFlavoured.MOD_ID + "/" + DIR + f;

            try (InputStream in = EnchantingScreen.class.getResourceAsStream(path)) {

                if (in != null) {

                    BufferedImage img = ImageIO.read(in);

                    if (img != null) {

                        return new int[] {img.getWidth(), img.getHeight()};
                    }

                }

            } catch (IOException ignored) {

                // fall through to the default size
            }
            return new int[] {fallbackW, fallbackH};
        });
    }



    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        super.extractBackground(graphics, mouseX, mouseY, delta);

        // 1. Base GUI (result slot disabled)
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,

            this.leftPos, this.topPos, 0.0F, 0.0F, GUI_W, GUI_H, TEX_W, TEX_H);



        // 2. Result slot "ready" overlay, plus the highlight while hovered.
        //    When the item is taken, hasResult() becomes false and both disappear again.
        if (this.menu.hasResult()) {

            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_READY, this.leftPos, this.topPos, 0.0F, 0.0F, GUI_W, GUI_H, TEX_W, TEX_H);

            int mx = mouseX - this.leftPos;
            int my = mouseY - this.topPos;
            boolean hovering = mx >= RESULT_X && mx < RESULT_X + RESULT_SIZE && my >= RESULT_Y && my < RESULT_Y + RESULT_SIZE;

            if (hovering) {

                graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_HOVER, this.leftPos, this.topPos, 0.0F, 0.0F, GUI_W, GUI_H, TEX_W, TEX_H);
            }
        }

        // 2b. Paint over the hint icons of the flower, lapis and centre slots once they hold an item.
        for (int gi : new int[] {Recipes.FLOWER_SLOTS[0], 7, Recipes.FLOWER_SLOTS[1], 4}) {

            net.minecraft.world.inventory.Slot slot = this.menu.slots.get(1 + gi);

            if (slot.hasItem()) {

                graphics.fill(this.leftPos + slot.x + 1, this.topPos + slot.y + 1,
                    this.leftPos + slot.x + 15, this.topPos + slot.y + 15, SLOT_COVER);
            }
        }

        // 3. Vanilla enchanting-table book (left side). It opens when the centre slot holds an item.
        VanillaBook.draw(graphics, this.leftPos, this.topPos, delta, this.menu.getCentreItem());

        // 4. XP orb + levels it costs, centred under the result slot. The level you must be at sits on the
        //    top-left corner of the result slot frame (26x26, starting 5px up/left of the item).
        int cost = this.menu.getCost();
        int required = this.menu.getRequiredLevel();

        if (cost > 0 && this.minecraft != null && this.minecraft.player != null) {

            boolean creative = this.minecraft.player.hasInfiniteMaterials();
            int level = this.minecraft.player.experienceLevel;
            boolean canPay = creative || level >= cost;
            boolean levelOk = creative || level >= required;

            int frameX = this.leftPos + RESULT_X - RESULT_BOX_PAD;
            int frameY = this.topPos + RESULT_Y - RESULT_BOX_PAD;

            // Orb + cost, centred under the result slot.
            int[] orb = sizeOf(canPay ? "xp_orb.png" : "xp_orb_disabled.png", 16, 16);
            Component number = Component.literal(String.valueOf(cost));
            int totalW = orb[0] + COST_GAP + this.font.width(number);
            int slotCentreX = this.leftPos + RESULT_X + RESULT_SIZE / 2;
            int x = slotCentreX - totalW / 2;
            int y = this.topPos + RESULT_Y + RESULT_SIZE + COST_BELOW_SLOT + 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, canPay ? ORB : ORB_DISABLED, x, y, 0.0F, 0.0F, orb[0], orb[1], orb[0], orb[1]);
            graphics.text(this.font, number, x + orb[0] + COST_GAP - 2, y + (orb[1] - 8) / 2, canPay ? COST_GREEN : COST_RED, true);

            // Top-left corner: required level, centred on the corner. Green when you are at that level, red when not.
            Component req = Component.literal(String.valueOf(required));
            graphics.text(this.font, req, frameX - this.font.width(req) / 2, frameY - 4, levelOk ? COST_GREEN : COST_RED, true);
        }



        // 5. Ghost recipe in the grid, the vanilla knowledge-book button, then the recipe-book panel.
        this.drawGhost(graphics);

        int bx = this.leftPos + BTN_X;
        int by = this.topPos + BTN_Y;

        sprite(graphics, this.overButton(mouseX, mouseY) ? V_BUTTON_HI : V_BUTTON, bx, by, BTN_W, BTN_H);

        if (this.panelOpen) {

            this.drawPanel(graphics, mouseX, mouseY);
        }
    }
}