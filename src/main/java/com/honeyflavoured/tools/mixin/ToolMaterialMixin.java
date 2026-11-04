package com.honeyflavoured.tools.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

@Mixin(ToolMaterial.class)
public abstract class ToolMaterialMixin {

    @Inject(method = "applyCommonProperties", at = @At("RETURN"))
    private void honeyflavoured$changeDurability(Item.Properties properties, CallbackInfoReturnable<Item.Properties> cir) {

        cir.getReturnValue().durability(1561);
    }
}