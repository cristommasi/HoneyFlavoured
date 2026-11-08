package com.honeyflavoured.horses;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

import net.minecraft.client.particle.HeartParticle;

public final class HorseCallClient {

	private HorseCallClient() {}

	public static void register() {

		ParticleProviderRegistry.getInstance().register(HorseCall.TAME_PARTICLE, HeartParticle.Provider::new);
	}
}