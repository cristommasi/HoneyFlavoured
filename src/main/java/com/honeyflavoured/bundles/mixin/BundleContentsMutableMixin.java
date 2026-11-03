package com.honeyflavoured.bundles.mixin;

import com.honeyflavoured.bundles.Bundles;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContents.Mutable.class)
public abstract class BundleContentsMutableMixin {

    @Inject(
        method = "findStackIndex",
        at = @At("RETURN"),
        cancellable = true
    )
    private void honeyflavoured$preventSmallStackMerging(
        ItemStack itemsToAdd,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (Bundles.enabled && itemsToAdd.getMaxStackSize() < 64) {
            cir.setReturnValue(-1);
        }
    }
}