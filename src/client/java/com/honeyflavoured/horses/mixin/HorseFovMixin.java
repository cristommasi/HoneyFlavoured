package com.honeyflavoured.horses.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class HorseFovMixin {

	@Inject(method = "getFieldOfViewModifier(ZF)F", at = @At("RETURN"), cancellable = true, require = 0)
	private void honeyflavoured$horseFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {

		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		Entity vehicle = self.getVehicle();

		if (!(vehicle instanceof AbstractHorse horse)) {

			return;
		}

		double base = horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
		double current = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
		float ratio;
		float factor;

		if (base <= 0.0D || current <= base) {

			return;
		}
		ratio = (float) (current / base);
		factor = (ratio + 1.0F) / 2.0F;
		cir.setReturnValue(cir.getReturnValue() * Mth.lerp(effectScale, 1.0F, factor));
	}
}