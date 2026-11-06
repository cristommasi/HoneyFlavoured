package com.honeyflavoured;

import com.honeyflavoured.animalarmor.AnimalArmor;
import com.honeyflavoured.commands.HoneyFlavouredCommands;
import com.honeyflavoured.enchanting.Enchanting;
import com.honeyflavoured.enchantments.NewEnchantments;
import com.honeyflavoured.fletching.Fletching;
import com.honeyflavoured.farming.Farming;

import com.honeyflavoured.temperature.TemperatureManager;

import net.fabricmc.api.ModInitializer;

/**
 * The only main entrypoint. It just calls each feature's setup, one line per feature.
 * To add another feature later, add its call here.
 */
public class HoneyFlavoured implements ModInitializer {

    public static final String MOD_ID = "honeyflavoured";

    @Override
    public void onInitialize() {

        HoneyFlavouredCommands.register();
        Enchanting.register();
        Fletching.register();
        Farming.register();
        NewEnchantments.register();
        TemperatureManager.register();
        AnimalArmor.register();
    }
}