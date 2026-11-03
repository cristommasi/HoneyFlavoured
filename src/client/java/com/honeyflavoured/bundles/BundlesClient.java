
package com.honeyflavoured.bundles;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class BundlesClient {
    private BundlesClient() {}

    public static void register() {

        // Register the payload type on the client.
        PayloadTypeRegistry.clientboundPlay().register(
            BundlesPayload.TYPE,
            BundlesPayload.CODEC
        );

        // Register the receiver.
        ClientPlayNetworking.registerGlobalReceiver(
            BundlesPayload.TYPE,
            (payload, context) ->
                Bundles.enabled = payload.enabled()
        );
    }
}
