package com.honeyflavoured.bundles;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;

/**
 * Bundles: bigger capacity for stackable items and for tools (see mixin/BundleContentsMixin).
 * Switched with /gamerule honeyflavoured:use_bundles <true|false>.
 * The rule is read on the server and sent to the clients, because the bundle tooltip is worked out on the client.
 */
public final class Bundles {
    private Bundles() {}

    /** Read by the mixin. Set by the server tick, and on the client by the packet. */
    public static volatile boolean enabled = true;

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(BundlesPayload.TYPE, BundlesPayload.CODEC);

        // Tell a player the current value as soon as they join.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            boolean now = HoneyFlavouredCommands.enabled(server.overworld(), HoneyFlavouredCommands.USE_BUNDLES);
            enabled = now;
            ServerPlayNetworking.send(handler.getPlayer(), new BundlesPayload(now));
        });

        // Watch the gamerule and tell everybody when it changes.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean now = HoneyFlavouredCommands.enabled(server.overworld(), HoneyFlavouredCommands.USE_BUNDLES);

            if (now != enabled) {
                enabled = now;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, new BundlesPayload(now));
                }
            }
        });
    }
}