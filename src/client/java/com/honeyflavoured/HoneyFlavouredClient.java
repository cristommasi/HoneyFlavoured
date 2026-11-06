package com.honeyflavoured;

import com.honeyflavoured.bundles.BundlesClient;
import com.honeyflavoured.enchanting.EnchantingClient;
import com.honeyflavoured.newenchantments.LavaBreathingHud;
import com.honeyflavoured.newenchantments.NewEnchantmentsClient;
import com.honeyflavoured.fletching.FletchingClient;
import com.honeyflavoured.temperature.TemperatureHud;

import net.fabricmc.api.ClientModInitializer;

public class HoneyFlavouredClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BundlesClient.register();

        EnchantingClient.register();
        FletchingClient.register();
        TemperatureHud.register();
        NewEnchantmentsClient.register();
        LavaBreathingHud.register();
    }
}