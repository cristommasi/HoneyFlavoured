package com.honeyflavoured.jukebox;

import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

public class JukeboxScreen extends AbstractContainerScreen<JukeboxMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("honeyflavoured",
            "textures/gui/jukebox/container/jukebox_menu.png");
    private static final Identifier PAUSE = Identifier.fromNamespaceAndPath("honeyflavoured",
            "textures/gui/jukebox/container/pause.png");

    private static final int GUI_W = 176;
    private static final int GUI_H = 222;   // double chest height
    private static final int TEX = 256;

    // x, y, width, height (from your coordinates)
    private static final int[] PREVIOUS = {56, 118, 18, 15};
    private static final int[] PLAY = {79, 115, 18, 19};
    private static final int[] NEXT = {102, 115, 18, 18};

    private static final int ARTIST_RIGHT = 77;   // artist ends here (left of the disc)
    private static final int TITLE_LEFT = 100;    // title starts here (right of the disc)
    private static final int TEXT_Y = 98;
    private static final int TEXT_MAX = 66;
    private static final int TEXT_COLOR = 0xFF404040;

    public JukeboxScreen(JukeboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_W, GUI_H);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos,
                0.0F, 0.0F, GUI_W, GUI_H, TEX, TEX);

        // highlight the disc that is playing
        int index = this.menu.getPlayingIndex();
        if (index >= 0 && index < JukeboxMenu.DISC_SLOTS && this.menu.isPlaying()) {
            Slot slot = this.menu.slots.get(index);
            graphics.fill(this.leftPos + slot.x, this.topPos + slot.y,
                    this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x6055FF55);
        }

        // pause icon replaces the play icon while playing
        if (this.menu.isPlaying()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PAUSE, this.leftPos + PLAY[0], this.topPos + PLAY[1],
                    0.0F, 0.0F, PLAY[2], PLAY[3], PLAY[2], PLAY[3]);
        }

        // artist on the left of the disc, title on the right
        ItemStack disc = this.menu.getNowPlaying();
        if (!disc.isEmpty()) {
            Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(disc);
            if (song.isPresent()) {
                Component description = song.get().value().description();
                String full = description.getString();
                int dash = full.indexOf(" - ");
                String artist = dash >= 0 ? full.substring(0, dash) : "";
                String name = dash >= 0 ? full.substring(dash + 3) : full;
                Style style = description.getStyle();

                if (!artist.isEmpty()) {
                    String shown = fit(artist);
                    Component text = Component.literal(shown).setStyle(style);
                    graphics.text(this.font, text, this.leftPos + ARTIST_RIGHT - this.font.width(shown),
                            this.topPos + TEXT_Y, TEXT_COLOR, false);
                }
                Component text = Component.literal(fit(name)).setStyle(style);
                graphics.text(this.font, text, this.leftPos + TITLE_LEFT, this.topPos + TEXT_Y, TEXT_COLOR, false);
            }
        }
    }

    private String fit(String text) {
        if (this.font.width(text) <= TEXT_MAX) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && this.font.width(cut + "...") > TEXT_MAX) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private boolean over(int[] box, double mx, double my) {
        double x = mx - this.leftPos;
        double y = my - this.topPos;
        return x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3];
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if ((event.button() == 0 || event.button() == 1) && this.minecraft != null) {
            int button = -1;
            if (over(PREVIOUS, event.x(), event.y())) {
                button = JukeboxMenu.BUTTON_PREVIOUS;
            } else if (over(PLAY, event.x(), event.y())) {
                button = JukeboxMenu.BUTTON_PLAY;
            } else if (over(NEXT, event.x(), event.y())) {
                button = JukeboxMenu.BUTTON_NEXT;
            }
            if (button >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}