package com.honeyflavoured.mounts;

import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

import net.minecraft.client.particle.HeartParticle;

public final class MountCallClient {

	private MountCallClient() {}

	public static void register() {

		ParticleProviderRegistry.getInstance().register(MountCall.TAME_PARTICLE, HeartParticle.Provider::new);
	}
}