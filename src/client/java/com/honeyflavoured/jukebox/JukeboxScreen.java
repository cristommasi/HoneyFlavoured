package com.honeyflavoured.jukebox;

import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class JukeboxScreen extends AbstractContainerScreen<JukeboxMenu> {
    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("honeyflavoured", "textures/gui/jukebox/container/" + name + ".png");
    }

    private static final Identifier BACKGROUND = tex("jukebox_menu");
    private static final Identifier PAUSE = tex("pause");
    private static final Identifier PAUSE_SELECT = tex("pause_select");
    private static final Identifier PLAY_SELECT = tex("play_select");
    private static final Identifier BACKWARD_SELECT = tex("backward_select");
    private static final Identifier FORWARD_SELECT = tex("forward_select");

    private static final int GUI_W = 176;
    private static final int GUI_H = 222;   // double chest height
    private static final int TEX = 256;     // canvas size of the PNGs; the overlays use the same size as the background

    // x, y, width, height
    private static final int[] BACKWARDS = {53, 110, 18, 18};
    private static final int[] PLAY = {79, 110, 18, 18};
    private static final int[] FORWARD = {105, 110, 18, 18};

    // "Artist - Song" line, to the right of the disc slot, vertically centred on it
    private static final int TEXT_X = 30;
    private static final int TEXT_Y = 20;
    private static final int TEXT_MAX = 139;   // visible width of the line
    private static final long SCROLL_PAUSE_MS = 1000L;
    private static final int SCROLL_SPEED = 30; // pixels per second

    public JukeboxScreen(JukeboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_W, GUI_H);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    /** Full-size background. */
    private void overlay(GuiGraphicsExtractor graphics, Identifier texture) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos, this.topPos,
                0.0F, 0.0F, GUI_W, GUI_H, TEX, TEX);
    }

    /** 18x18 button image drawn on top of its button. */
    private void button(GuiGraphicsExtractor graphics, Identifier texture, int[] box) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos + box[0], this.topPos + box[1],
                0.0F, 0.0F, box[2], box[3], box[2], box[3]);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        overlay(graphics, BACKGROUND); // contains the normal backwards / play / forward buttons

        // highlight the disc that is playing
        int index = this.menu.getPlayingIndex();
        boolean playing = this.menu.isPlaying();
        if (index >= 0 && index < JukeboxMenu.DISC_SLOTS && playing) {
            Slot slot = this.menu.slots.get(index);
            graphics.fill(this.leftPos + slot.x, this.topPos + slot.y,
                    this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x80FFFFFF);
        }

        // buttons: swap to the _select version while hovering
        boolean overPlay = over(PLAY, mouseX, mouseY);
        if (playing) {
            button(graphics, overPlay ? PAUSE_SELECT : PAUSE, PLAY);
        } else if (overPlay) {
            button(graphics, PLAY_SELECT, PLAY);
        }
        if (over(BACKWARDS, mouseX, mouseY)) {
            button(graphics, BACKWARD_SELECT, BACKWARDS);
        }
        if (over(FORWARD, mouseX, mouseY)) {
            button(graphics, FORWARD_SELECT, FORWARD);
        }

        // "Artist - Song": rainbow colour, scrolls sideways when it doesn't fit
        ItemStack disc = this.menu.getNowPlaying();
        if (!disc.isEmpty()) {
            Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(disc);
            if (song.isPresent()) {
                String name = song.get().value().description().getString();
                float hue = (System.currentTimeMillis() % 3000L) / 3000.0F;
                int color = 0xFF000000 | (Mth.hsvToRgb(hue, 0.7F, 0.6F) & 0xFFFFFF);

                int x = this.leftPos + TEXT_X;
                int y = this.topPos + TEXT_Y;
                int overflow = this.font.width(name) - TEXT_MAX;
                int offset = 0;

                if (overflow > 0) {
                    long scrollMs = overflow * 1000L / SCROLL_SPEED;
                    long t = System.currentTimeMillis() % (SCROLL_PAUSE_MS * 2 + scrollMs);
                    if (t >= SCROLL_PAUSE_MS + scrollMs) {
                        offset = overflow;
                    } else if (t >= SCROLL_PAUSE_MS) {
                        offset = (int) ((t - SCROLL_PAUSE_MS) * SCROLL_SPEED / 1000L);
                    }
                }

                graphics.enableScissor(x, y - 2, x + TEXT_MAX, y + 10);
                graphics.text(this.font, Component.literal(name), x - offset, y, color, false);
                graphics.disableScissor();
            }
        }
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
            if (over(BACKWARDS, event.x(), event.y())) {
                button = JukeboxMenu.BUTTON_PREVIOUS;
            } else if (over(PLAY, event.x(), event.y())) {
                button = JukeboxMenu.BUTTON_PLAY;
            } else if (over(FORWARD, event.x(), event.y())) {
                button = JukeboxMenu.BUTTON_NEXT;
            }
            if (button >= 0) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}