package com.honeyflavoured.horses.mixin;

import com.honeyflavoured.horses.HorseSink;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class HorseBoxMixin {

    @Inject(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true, require = 0)
    private void honeyflavoured$lowerBox(Vec3 position, CallbackInfoReturnable<AABB> cir) {

        if (this instanceof HorseSink horse) {

            float sink = horse.honeyflavoured$getSink(1.0F);

            if (sink > 0.0F) {

                AABB box = cir.getReturnValue();

                cir.setReturnValue(new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY - sink, box.maxZ));
            }
        }
    }
    @Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true, require = 0)
    private void honeyflavoured$lowerEyes(CallbackInfoReturnable<Float> cir) {

        if (this instanceof HorseSink horse) {

            cir.setReturnValue(cir.getReturnValue() - horse.honeyflavoured$getSink(1.0F));
        }
    }
}