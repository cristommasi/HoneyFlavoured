package com.honeyflavoured.enchantments.mixin;

import com.honeyflavoured.enchantments.NewEnchantments;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class LavaMiningMixin {

    /** Vanilla's underwater mining multiplier (the submerged_mining_speed base). */
    private static final float SUBMERGED_FACTOR = 0.2F;

    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("RETURN"), cancellable = true)
    private void honeyflavoured$lavaMining(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;

        if (!NewEnchantments.enabled || !self.isEyeInFluid(FluidTags.LAVA)) {
            return;
        }

        // Aqua Affinity does not help in lava. Only Lava Affinity removes the penalty.
        if (NewEnchantments.armorLevel(self, NewEnchantments.LAVA_AFFINITY) > 0) {
            return;
        }

        cir.setReturnValue(cir.getReturnValue() * SUBMERGED_FACTOR);
    }
}