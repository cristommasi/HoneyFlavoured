package com.honeyflavoured.enchantments.mixin;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public abstract class SpawnerPlaceMixin {

	@Inject(method = "onlyOpCanSetNbt", at = @At("HEAD"), cancellable = true)
	private void honeyflavoured$allowPlacedSpawnerData(CallbackInfoReturnable<Boolean> cir) {

		if ((Object) this == BlockEntityTypes.MOB_SPAWNER) {

			cir.setReturnValue(false);
		}
	}
}