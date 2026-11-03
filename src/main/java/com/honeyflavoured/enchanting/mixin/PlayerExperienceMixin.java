package com.honeyflavoured.enchanting.mixin;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerExperienceMixin {

    private static final int XP_PER_LEVEL = 16;

    @Inject(method = "getXpNeededForNextLevel", at = @At("HEAD"), cancellable = true)
    private void HoneyFlavoured$changeXpCurve(CallbackInfoReturnable<Integer> cir) {
        Player self = (Player) (Object) this;

        // Gamerule honeyflavoured:modded_xp false = vanilla XP curve.
        if (!HoneyFlavouredCommands.enabled(self.level(), HoneyFlavouredCommands.USE_NEW_XP_CURVE)) {
            return;
        }

        cir.setReturnValue(XP_PER_LEVEL);
    }

}