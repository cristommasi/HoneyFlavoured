package com.honeyflavoured.animalarmor.mixin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class AbstractMountMixin {
    @Shadow protected SimpleContainer inventory;
    @Shadow protected abstract void createInventory();

    @Inject(method = "tick", at = @At("TAIL"))
    private void honeyflavoured$resize(CallbackInfo ci) {

        AbstractHorse self = (AbstractHorse) (Object) this;
        
        if (!(self instanceof AbstractChestedHorse)) return;
        if (self.tickCount % 20 != 0) return;

        int expected = self.getInventorySize();
        int current = inventory.getContainerSize();
        if (current == expected) return;

        createInventory(); // vanilla copies everything that still fits
    }
}