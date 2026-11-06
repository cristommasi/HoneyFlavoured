package com.honeyflavoured.animalarmor.mixin;

import com.honeyflavoured.animalarmor.AnimalArmor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfShearMixin {

    @Inject(method = "canShearEquipment(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("RETURN"), cancellable = true)
    private void honeyflavoured$bindingCurse(Player player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack armor = ((Wolf) (Object) this).getItemBySlot(EquipmentSlot.BODY);

        if (!player.isCreative() && !armor.isEmpty() && AnimalArmor.isBound(armor)) {
            cir.setReturnValue(false);
        }
    }
}