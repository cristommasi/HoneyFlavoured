package com.honeyflavoured.bundles;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: is the use_bundles gamerule on? */
public record BundlesPayload(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BundlesPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("honeyflavoured", "bundles"));

    public static final StreamCodec<ByteBuf, BundlesPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, BundlesPayload::enabled, BundlesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}