
package com.honeyflavoured.enchanting;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

public class EnchantingClient {

    private EnchantingClient() {}

    public static void register() {

        MenuScreens.register(Enchanting.MENU_TYPE, EnchantingScreen::new);

        // "New Enchantment Unlocked" toast.
        ClientPlayNetworking.registerGlobalReceiver(UnlockPayload.TYPE, (payload, context) -> {

            Minecraft mc = Minecraft.getInstance();

            if (mc.level == null || payload.index() < 0 || payload.index() >= Recipes.ORDERED.size()) {
                return;
            }

            var enchantmentKey = Recipes.ORDERED.get(payload.index()).getValue();

            Identifier icon = Identifier.fromNamespaceAndPath(
                    "honeyflavoured",
                    "textures/gui/enchanting/enchantments/" + enchantmentKey.identifier().getPath() + ".png"
            );

            mc.gui.toastManager().addToast(new EnchantingRecipeToast(icon));
        });
    }
}
