
package com.honeyflavoured.bundles.mixin;

import com.honeyflavoured.bundles.Bundles;

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.component.BundleContents;

import org.apache.commons.lang3.math.Fraction;
import com.mojang.serialization.DataResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContents.class)
public abstract class BundleContentsMixin {

    private static final Fraction STACKABLE =
        Fraction.getFraction(1, 64);

    private static final Fraction UNSTACKABLE =
        Fraction.getFraction(1, 8);

    @Inject(
        method = "getWeight(Lnet/minecraft/world/item/ItemInstance;)Lcom/mojang/serialization/DataResult;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void honeyflavoured$bundleWeight(
        ItemInstance item,
        CallbackInfoReturnable<DataResult<Fraction>> cir
    ) {
        if (!Bundles.enabled) {
            return;
        }

        Fraction weight = item.getMaxStackSize() == 1
            ? UNSTACKABLE
            : STACKABLE;

        cir.setReturnValue(DataResult.success(weight));
    }
}
