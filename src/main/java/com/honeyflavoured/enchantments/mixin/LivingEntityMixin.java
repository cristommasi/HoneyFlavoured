package com.honeyflavoured.enchantments.mixin;

import com.honeyflavoured.commands.HoneyFlavouredCommands;
import com.honeyflavoured.enchantments.NewEnchantments;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
public abstract class LivingEntityMixin {

    /**
     * Lava leaves you at about a fiftieth of walking speed. Igneous Strider adds acceleration and keeps more speed
     * each tick, like Depth Strider does in water. Raise EXTRA_ACCELERATION to go faster (0.01 is about walking pace).
     */
    @Unique private static final float EXTRA_ACCELERATION = 0.01F;
    @Unique private static final double LAVA_SPEED_BOOST = 1.6;

    /** Air supply at the start of the tick, so we can undo vanilla refilling it while the head is in lava. */
    @Unique private int honeyflavoured$airBefore;

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

    @Inject(method = "baseTick()V", at = @At("HEAD"))
    private void honeyflavoured$rememberAir(CallbackInfo ci) {
        this.honeyflavoured$airBefore = ((LivingEntity) (Object) this).getAirSupply();
    }

    /**
     * Holding your breath in lava: while your head is in lava the air bubbles drain exactly like under water
     * (Respiration slows it down) and you take drowning damage when they run out.
     * Gamerule honeyflavoured:use_lava_breathing.
     */
    @Inject(method = "baseTick()V", at = @At("TAIL"))
    private void honeyflavoured$lavaBreath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self.level() instanceof ServerLevel level) || !(self instanceof Player player) || !player.isAlive()
            || player.isCreative() || player.isSpectator()
            || !HoneyFlavouredCommands.enabled(level, HoneyFlavouredCommands.USE_LAVA_BREATHING)
            || !player.isEyeInFluid(FluidTags.LAVA)) {
            return;
        }

        // Same rule as vanilla: Respiration makes some ticks cost no air.
        double oxygenBonus = player.getAttributeValue(Attributes.OXYGEN_BONUS);
        boolean free = oxygenBonus > 0.0 && player.getRandom().nextDouble() >= 1.0 / (oxygenBonus + 1.0);
        int air = free ? this.honeyflavoured$airBefore : this.honeyflavoured$airBefore - 1;

        if (air <= -20) {
            air = 0;
            player.hurtServer(level, player.damageSources().drown(), 2.0F);
        }

        player.setAirSupply(air);
    }
}