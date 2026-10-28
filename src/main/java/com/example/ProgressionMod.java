package com.example;

import com.example.craftenchanting.CraftEnchanting;
import com.example.fletching.Fletching;

import net.fabricmc.api.ModInitializer;

/**
 * The only main entrypoint. It just calls each feature's setup, one line per feature.
 * To add another feature later, add its call here.
 */
public class ProgressionMod implements ModInitializer {
	@Override
	public void onInitialize() {
		new CraftEnchanting().onInitialize();   // enchanting table
		Fletching.register();                    // fletching table
	}
}