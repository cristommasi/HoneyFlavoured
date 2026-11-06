package com.honeyflavoured.newenchantments;


import com.honeyflavoured.enchantments.NewEnchantments;
import com.honeyflavoured.enchantments.NewEnchantmentsPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client side: remembers whether the server has use_new_enchantments on (Igneous Strider is worked out on the client). */
public final class NewEnchantmentsClient {
    private NewEnchantmentsClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NewEnchantmentsPayload.TYPE,
            (payload, context) -> NewEnchantments.enabled = payload.enabled());
    }
}