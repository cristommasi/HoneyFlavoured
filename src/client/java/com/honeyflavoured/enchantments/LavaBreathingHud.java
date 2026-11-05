package com.honeyflavoured.enchantments;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class LavaBreathingHud {
    private LavaBreathingHud() {}

    private static final String DIR = "textures/gui/enchantments/container/";   // <- the folder your 3 files are in
    private static final Identifier AIR = tex("lava_air");
    private static final Identifier EMPTY = tex("lava_air_empty");
    private static final Identifier BURSTING = tex("lava_air_bursting");

    /** True from the moment the head is in lava until the air has refilled. */
    private static boolean showing;

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath("honeyflavoured", DIR + name + ".png");
    }

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, original -> (graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (player == null) {
                original.extractRenderState(graphics, tickCounter);
                return;
            }

            int max = player.getMaxAirSupply();
            int air = player.getAirSupply();

            if (player.isEyeInFluid(FluidTags.LAVA) && air < max) {
                showing = true;
            }
            else if (air >= max) {
                showing = false;
            }

            if (!showing) {
                original.extractRenderState(graphics, tickCounter);   // normal water bubbles
                return;
            }

            int full = Mth.ceil((air - 2) * 10.0 / max);
            int partial = Mth.ceil(air * 10.0 / max) - full;
            int x0 = graphics.guiWidth() / 2 + 91 - 9;
            int y = graphics.guiHeight() - 49;

            for (int i = 0; i < 10; i++) {
                int x = x0 - i * 8;
                Identifier sprite = i < full ? AIR : (i < full + partial ? BURSTING : EMPTY);

                graphics.blit(RenderPipelines.GUI_TEXTURED, sprite, x, y, 0, 0, 9, 9, 9, 9);
            }
        });
    }
}