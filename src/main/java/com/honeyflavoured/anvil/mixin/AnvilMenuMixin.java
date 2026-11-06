package com.honeyflavoured.anvil.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.honeyflavoured.enchantments.NewEnchantments;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow @Final private DataSlot cost;

    @Inject(method = "createResult()V", at = @At("TAIL"))
    private void honeyflavoured$enforceRules(CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ItemStack result = menu.getSlot(2).getItem();

        if (NewEnchantments.violatesRules(result)) {
            menu.getSlot(2).set(ItemStack.EMPTY);
            this.cost.set(0);
        }
    }
}
