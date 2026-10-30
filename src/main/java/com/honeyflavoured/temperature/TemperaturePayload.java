package com.honeyflavoured.temperature;

import com.honeyflavoured.HoneyFlavoured;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: current cold / hot exposure (0.0 - 1.0). */
public record TemperaturePayload(float cold, float hot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TemperaturePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "temperature_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TemperaturePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, TemperaturePayload::cold,
        ByteBufCodecs.FLOAT, TemperaturePayload::hot,
        TemperaturePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}