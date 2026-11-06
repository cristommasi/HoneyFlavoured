package com.honeyflavoured.animalarmor.mixin;

import com.honeyflavoured.animalarmor.AnimalArmor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotBindingMixin {

    @Inject(method = "mayPickup(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void honeyflavoured$bindingCurse(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ArmorSlot) || player.isCreative()) {
            return;
        }

        ItemStack stack = ((Slot) (Object) this).getItem();

        if (!stack.isEmpty() && AnimalArmor.isAnimalArmor(stack.getItem()) && AnimalArmor.isBound(stack)) {
            cir.setReturnValue(false);
        }
    }
}