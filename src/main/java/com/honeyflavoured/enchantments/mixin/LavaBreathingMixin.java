
package com.honeyflavoured.enchantments.mixin;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LavaBreathingMixin {

    @Unique private int honeyflavoured$airBefore;

    @Inject(method = "baseTick()V", at = @At("HEAD"))
    private void honeyflavoured$rememberAir(CallbackInfo ci) {
        this.honeyflavoured$airBefore = ((LivingEntity) (Object) this).getAirSupply();
    }

    @Inject(method = "baseTick()V", at = @At("TAIL"))
    private void honeyflavoured$lavaBreath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self.level() instanceof ServerLevel level) || !(self instanceof Player player) || !player.isAlive()
            || player.isCreative() || player.isSpectator()
            || !HoneyFlavouredCommands.enabled(level, HoneyFlavouredCommands.USE_LAVA_BREATHING)
            || !player.isEyeInFluid(FluidTags.LAVA)) {
            return;
        }

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
