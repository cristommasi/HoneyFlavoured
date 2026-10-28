package com.example.craftenchanting.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerExperienceMixin {

    private static final int XP_PER_LEVEL = 18;

    @Inject(method = "getXpNeededForNextLevel", at = @At("HEAD"), cancellable = true)
    private void craftEnchanting$changeXpCurve(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(XP_PER_LEVEL);
    }

}