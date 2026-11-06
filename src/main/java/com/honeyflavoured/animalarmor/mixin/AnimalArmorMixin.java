package com.honeyflavoured.animalarmor.mixin;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class AnimalArmorMixin {

    @Shadow protected abstract void doHurtEquipment(DamageSource source, float damage, EquipmentSlot... slots);

    @Inject(method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))
    private void honeyflavoured$animalArmor(DamageSource source, float damage, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof Player || !(self.level() instanceof ServerLevel level)
            || !HoneyFlavouredCommands.enabled(level, HoneyFlavouredCommands.USE_ANIMAL_ARMOR)) {
            return;
        }

        ItemStack body = self.getItemBySlot(EquipmentSlot.BODY);

        if (!body.isEmpty() && body.isDamageableItem()) {
            this.doHurtEquipment(source, damage, EquipmentSlot.BODY);
        }
    }
}