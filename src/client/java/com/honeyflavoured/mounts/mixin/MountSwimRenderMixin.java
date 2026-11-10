package com.honeyflavoured.mounts.mixin;

import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.honeyflavoured.mounts.MountSink;

@Mixin(AbstractHorseRenderer.class)
public abstract class MountSwimRenderMixin {

	private static final float PADDLE_SPEED = 1.0F;
	private static final float MIN_PADDLE_AMOUNT = 0.7F;

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/equine/AbstractHorse;Lnet/minecraft/client/renderer/entity/state/EquineRenderState;F)V", at = @At("TAIL"))
	private void honeyflavoured$swimPose(AbstractHorse entity, EquineRenderState state, float partialTicks, CallbackInfo ci) {

		float sink = ((MountSink) entity).honeyflavoured$getSink(partialTicks);

		if (sink <= 0.0F) {

			return;
		}
		state.y -= sink;
		if (sink < 0.05F) {

			return;
		}
		state.isInWater = false;
		state.walkAnimationPos = state.ageInTicks * PADDLE_SPEED;
		state.walkAnimationSpeed = Math.max(state.walkAnimationSpeed, MIN_PADDLE_AMOUNT);
	}
}