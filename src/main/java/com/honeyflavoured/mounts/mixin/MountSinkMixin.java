package com.honeyflavoured.mounts.mixin;

import net.minecraft.world.entity.animal.equine.AbstractHorse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.honeyflavoured.mounts.MountSink;

@Mixin(AbstractHorse.class)
public abstract class MountSinkMixin implements MountSink {

	@Unique
	private static final float MAX_SINK = 0.7F;
	@Unique
	private static final float SINK_RISE = 0.08F;
	@Unique
	private static final float SINK_FALL = 0.04F;
	@Unique
	private float honeyflavoured$sink;
	@Unique
	private float honeyflavoured$sinkO;

	@Inject(method = "tick", at = @At("TAIL"))
	private void honeyflavoured$tickSink(CallbackInfo ci) {

		AbstractHorse self = (AbstractHorse) (Object) this;
		boolean swimming = self.isInWater() && !self.onGround();

		this.honeyflavoured$sinkO = this.honeyflavoured$sink;
		if (swimming) {

			this.honeyflavoured$sink = Math.min(MAX_SINK, this.honeyflavoured$sink + SINK_RISE);
		} else {

			this.honeyflavoured$sink = Math.max(0.0F, this.honeyflavoured$sink - SINK_FALL);
		}
	}

	@Override
	public float honeyflavoured$getSink(float partialTicks) {

		return (this.honeyflavoured$sinkO + (this.honeyflavoured$sink - this.honeyflavoured$sinkO) * partialTicks);
	}
}