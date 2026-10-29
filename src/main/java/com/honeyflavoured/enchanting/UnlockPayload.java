package com.honeyflavoured.enchanting;

import com.honeyflavoured.HoneyFlavoured;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: "recipe #index was just unlocked" so the client can show the toast. */
public record UnlockPayload(int index) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<UnlockPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(HoneyFlavoured.MOD_ID, "recipe_unlocked"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UnlockPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, UnlockPayload::index, UnlockPayload::new);

	@Override
	public CustomPacketPayload.Type<UnlockPayload> type() {

		return TYPE;
	}
}