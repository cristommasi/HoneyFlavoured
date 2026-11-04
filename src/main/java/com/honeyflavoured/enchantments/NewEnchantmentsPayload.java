package com.honeyflavoured.enchantments;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: is the use_new_enchantments gamerule on? */
public record NewEnchantmentsPayload(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NewEnchantmentsPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("honeyflavoured", "new_enchantments"));

    public static final StreamCodec<ByteBuf, NewEnchantmentsPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, NewEnchantmentsPayload::enabled, NewEnchantmentsPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}