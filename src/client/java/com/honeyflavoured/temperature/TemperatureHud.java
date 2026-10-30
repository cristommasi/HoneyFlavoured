package com.honeyflavoured.temperature;

import com.honeyflavoured.HoneyFlavoured;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class TemperatureHud {

    private TemperatureHud() {}

    /* The four textures a heart type needs, like vanilla's frozen hearts: full, half, and their white blinking flashes. */
    private record HeartSet(Identifier full, Identifier half, Identifier fullBlink, Identifier halfBlink) {}

    private static Identifier vanillaHeart(String name) {
        return Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/" + name + ".png");
    }

    private static Identifier ownHeart(String name) {
        return Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "textures/gui/temperature/container/" + name + ".png");
    }

    /* Cold = vanilla's own frozen hearts. */
    private static final HeartSet COLD_HEARTS = new HeartSet(
        vanillaHeart("frozen_full"), vanillaHeart("frozen_half"),
        vanillaHeart("frozen_full_blinking"), vanillaHeart("frozen_half_blinking"));

    private static final HeartSet COLD_HEARTS_HARDCORE = new HeartSet(
        vanillaHeart("frozen_hardcore_full"), vanillaHeart("frozen_hardcore_half"),
        vanillaHeart("frozen_hardcore_full_blinking"), vanillaHeart("frozen_hardcore_half_blinking"));

    /* Hot = your burnt hearts, in textures/gui/temperature/container/. */
    private static final HeartSet HOT_HEARTS = new HeartSet(
        ownHeart("burnt_full"), ownHeart("burnt_half"),
        ownHeart("burnt_full_blinking"), ownHeart("burnt_half_blinking"));

    private static final HeartSet HOT_HEARTS_HARDCORE = new HeartSet(
        ownHeart("burnt_hardcore_full"), ownHeart("burnt_hardcore_half"),
        ownHeart("burnt_hardcore_full_blinking"), ownHeart("burnt_hardcore_half_blinking"));

    private static float cold;
    private static float hot;
    private static float coldVision;
    private static float hotVision;
    private static long lastVisionUpdate;

    // These mirror vanilla's health animation state.
    private static int lastHealth = -1;
    private static int displayHealth = -1;
    private static long lastHealthTime;
    private static long healthBlinkTime;

    private static final RandomSource HEART_RANDOM = RandomSource.create();
    private static final Identifier COLD_OVERLAY = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static final Identifier HOT_OVERLAY = Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "textures/gui/temperature/container/hot_overlay.png");


    public static void register() {

        ClientPlayNetworking.registerGlobalReceiver(TemperaturePayload.TYPE, (payload, context) -> {
            cold = payload.cold();
            hot = payload.hot();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            cold = hot = coldVision = hotVision = 0.0F;
            lastVisionUpdate = 0L;
            lastHealth = -1;
            displayHealth = -1;
            lastHealthTime = 0L;
            healthBlinkTime = 0L;
        });


        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "temperature_vision"), TemperatureHud::renderTemperatureVision);
        
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (graphics, tickCounter) -> {
            original.extractRenderState(graphics, tickCounter);
            renderHearts(graphics, tickCounter);
        });
    }

    private static void renderHearts(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) return;

        long time = Util.getMillis();

        float exposure = Math.max(cold, hot);
        if (exposure <= 0.0F) return;

        boolean hardcore = client.level != null && client.level.getLevelData().isHardcore();
        HeartSet hearts = cold >= hot
            ? (hardcore ? COLD_HEARTS_HARDCORE : COLD_HEARTS)
            : (hardcore ? HOT_HEARTS_HARDCORE : HOT_HEARTS);

        int guiTicks = client.gui.hud.getGuiTicks();
        int currentHealth = Mth.ceil(player.getHealth());

        if (lastHealth < 0) {
            lastHealth = currentHealth;
            displayHealth = currentHealth;
            lastHealthTime = time;
        }

        boolean blink = healthBlinkTime > guiTicks && (healthBlinkTime - guiTicks) / 3L % 2L == 1L;

        if (currentHealth < lastHealth && player.damageCooldownTime > 0) {
            lastHealthTime = time;
            healthBlinkTime = guiTicks + 20;
        } else if (currentHealth > lastHealth && player.damageCooldownTime > 0) {
            lastHealthTime = time;
            healthBlinkTime = guiTicks + 10;
        }

        if (time - lastHealthTime > 1000L) {
            displayHealth = currentHealth;
            lastHealthTime = time;
        }

        lastHealth = currentHealth;
        int oldHealth = displayHealth;

        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), Math.max(oldHealth, currentHealth));
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int healthContainerCount = Mth.ceil(maxHealth / 2.0F);
        int absorptionContainerCount = Mth.ceil(absorption / 2.0F);
        int totalRows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (totalRows - 2), 3);

        int maxHearts = Mth.ceil(player.getMaxHealth() / 2.0F);
        int currentHearts = Mth.ceil(currentHealth / 2.0F);


        int changedHearts = Math.min(currentHearts, Mth.ceil(maxHearts * exposure));
        int blinkHearts = Math.min(Mth.ceil(Math.max(currentHealth, oldHealth) / 2.0F), Mth.ceil(maxHearts * exposure));
        if (changedHearts <= 0) return;

        int xLeft = graphics.guiWidth() / 2 - 91;
        int yBase = graphics.guiHeight() - 39;

        int heartOffsetIndex = player.hasEffect(MobEffects.REGENERATION) ? guiTicks % Mth.ceil(maxHealth + 5.0F) : -1;

        HEART_RANDOM.setSeed(guiTicks * 312871);

        for (int index = healthContainerCount + absorptionContainerCount - 1; index >= 0; index--) {
            int row = index / 10;
            int column = index % 10;
            int x = xLeft + column * 8;
            int y = yBase - row * rowHeight;

            if (currentHealth + absorption <= 4) y += HEART_RANDOM.nextInt(2);
            if (index < healthContainerCount && index == heartOffsetIndex) y -= 2;

            if (index >= healthContainerCount) continue;

            int halves = index * 2;

            // Same order as vanilla: first the blinking heart for the health you just lost or gained.
            // A heart you just lost is past changedHearts, so it has its own limit.
            if (blink && halves < oldHealth && index < blinkHearts) {
                Identifier blinkTexture = halves + 1 == oldHealth ? hearts.halfBlink() : hearts.fullBlink();
                graphics.blit(RenderPipelines.GUI_TEXTURED, blinkTexture, x, y, 0, 0, 9, 9, 9, 9);
            }

            // ...then the normal heart on top of it for the health you still have.
            if (index >= changedHearts || halves >= currentHealth) continue;

            boolean half = halves + 1 == currentHealth;
            graphics.blit(RenderPipelines.GUI_TEXTURED, half ? hearts.half() : hearts.full(), x, y, 0, 0, 9, 9, 9, 9);
        }
    }

    private static float getVisionStrength(float exposure, LocalPlayer player) {
        float health = player.getHealth();
        if (health <= 0.0F) return 0.0F;

        float affectedHealth = exposure * player.getMaxHealth();
        float start = health * 0.60F;

        return Mth.clamp((affectedHealth - start) / Math.max(0.001F, health - start), 0.0F, 1.0F);
    }

    private static void updateVision() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) {
            coldVision = hotVision = 0.0F;
            lastVisionUpdate = 0L;
            return;
        }

        long now = System.nanoTime();
        float delta = lastVisionUpdate == 0L ? 1.0F / 60.0F : Mth.clamp((now - lastVisionUpdate) / 1_000_000_000.0F, 0.0F, 0.1F);
        lastVisionUpdate = now;

        float smoothing = 1.0F - (float)Math.exp(-delta * 3.0F);

        coldVision = Mth.lerp(smoothing, coldVision, getVisionStrength(cold, player));
        hotVision = Mth.lerp(smoothing, hotVision, getVisionStrength(hot, player));
    }

    private static void renderTemperatureVision(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        updateVision();

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        int coldAlpha = Mth.clamp(Math.round(coldVision * 255.0F), 0, 255);
        int hotAlpha = Mth.clamp(Math.round(hotVision * 255.0F), 0, 255);

        if (coldAlpha > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, COLD_OVERLAY, 0, 0, 0, 0, width, height, 256, 256, 256, 256, coldAlpha << 24 | 0xFFFFFF);
        if (hotAlpha > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, HOT_OVERLAY, 0, 0, 0, 0, width, height, 256, 256, 256, 256, hotAlpha << 24 | 0xFFFFFF);
    }
}