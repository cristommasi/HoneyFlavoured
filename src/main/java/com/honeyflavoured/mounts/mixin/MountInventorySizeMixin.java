
package com.honeyflavoured.mounts.mixin;

import com.honeyflavoured.mounts.MountInventory;

import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Mule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public abstract class MountInventorySizeMixin {

    @Inject(method = "getInventorySize", at = @At("RETURN"), cancellable = true)
    private void honeyflavoured$muleInventorySize(CallbackInfoReturnable<Integer> cir) {

        AbstractHorse horse = (AbstractHorse) (Object) this;

        if (horse instanceof Mule && horse.getInventoryColumns() > 0) {

            cir.setReturnValue(MountInventory.MULE_SIZE);
        }
    }
}
