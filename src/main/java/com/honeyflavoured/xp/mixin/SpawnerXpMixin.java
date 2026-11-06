package com.honeyflavoured.xp.mixin;

import com.honeyflavoured.enchantments.enchantments.SilkTouch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerXpMixin {

	@Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
	private void honeyflavoured$noXpOnSilk(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience, CallbackInfo ci) {

		if (SilkTouch.hasSilkTouch(level, tool)) {

			ci.cancel();
		}
	}
}