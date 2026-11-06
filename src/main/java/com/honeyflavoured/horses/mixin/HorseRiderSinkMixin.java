package com.honeyflavoured.horses.mixin;

import com.honeyflavoured.horses.HorseSink;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class HorseRiderSinkMixin {

	@Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
	private void honeyflavoured$sinkRider(Entity passenger, EntityDimensions dimensions, float scale, CallbackInfoReturnable<Vec3> cir) {

		float sink = ((HorseSink) this).honeyflavoured$getSink(1.0F);

		if (sink > 0.0F) {

			cir.setReturnValue(cir.getReturnValue().add(0.0D, -sink * scale, 0.0D));
		}
	}
}