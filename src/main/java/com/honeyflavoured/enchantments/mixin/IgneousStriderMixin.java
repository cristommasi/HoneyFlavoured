
package com.honeyflavoured.enchantments.mixin;

import com.honeyflavoured.enchantments.NewEnchantments;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class IgneousStriderMixin {

    @Unique private static final float EXTRA_ACCELERATION = 0.01F;
    @Unique private static final double LAVA_SPEED_BOOST = 1.6;

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void honeyflavoured$igneousStrider(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!NewEnchantments.enabled || !(self instanceof Player) || !self.isInLava()) {
            return;
        }

        Holder<Enchantment> igneous = self.level().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(NewEnchantments.IGNEOUS_STRIDER);

        if (EnchantmentHelper.getItemEnchantmentLevel(igneous, self.getItemBySlot(EquipmentSlot.FEET)) <= 0) {
            return;
        }

        self.moveRelative(EXTRA_ACCELERATION, input);

        Vec3 motion = self.getDeltaMovement();
        self.setDeltaMovement(motion.x * LAVA_SPEED_BOOST, motion.y, motion.z * LAVA_SPEED_BOOST);
    }
}
