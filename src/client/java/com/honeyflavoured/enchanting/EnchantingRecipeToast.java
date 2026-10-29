
package com.honeyflavoured.enchanting;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EnchantingRecipeToast implements Toast {

    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/recipe");

    private static final long DISPLAY_TIME = 5000L;

    private static final Component TITLE_TEXT = Component.literal("New Enchantment Unlocked");

    private static final Component DESCRIPTION_TEXT = Component.literal("Check your recipe book");

    private static final int TOAST_WIDTH = 200;
    private static final int TOAST_HEIGHT = 32;

    private final ItemStack craftingStation =
            new ItemStack(Items.ENCHANTING_TABLE);

    private final Identifier iconTexture;

    private long lastChanged;
    private boolean changed = true;

    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public EnchantingRecipeToast(Identifier iconTexture) {
        this.iconTexture = iconTexture;
    }

    @Override
    public int width() {

        return TOAST_WIDTH;
    }

    @Override
    public int height() {

        return TOAST_HEIGHT;
    }

    @Override
    public Toast.Visibility getWantedVisibility() {

        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {

        if (this.changed) {
            this.lastChanged = fullyVisibleForMs;
            this.changed = false;
        }
        this.wantedVisibility = fullyVisibleForMs - this.lastChanged >= DISPLAY_TIME * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public void extractRenderState( GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,BACKGROUND_SPRITE,0,0,this.width(),this.height());

        // Toast title and description.
        graphics.text(font, TITLE_TEXT, 30, 7, -11534256, false);
        graphics.text(font, DESCRIPTION_TEXT, 30, 18, -16777216, false);

        // Vanilla enchanting table icon.
        graphics.pose().pushMatrix();
        graphics.pose().scale(0.6F, 0.6F);
        graphics.fakeItem(craftingStation, 3, 3);
        graphics.pose().popMatrix();

        // Custom icon for the unlocked enchantment.
        graphics.blit( RenderPipelines.GUI_TEXTURED, iconTexture, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
    }
}
