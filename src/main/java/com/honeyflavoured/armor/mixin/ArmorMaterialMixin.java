package com.honeyflavoured.armor.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

@Mixin(ArmorMaterial.class)
public abstract class ArmorMaterialMixin {

    @Shadow
    @Final
    private int durability;

    @Inject( method = "durability", at = @At("HEAD"), cancellable = true)
    private void honeyflavoured$changeDurability(CallbackInfoReturnable<Integer> cir) {
        if (this.durability == 37) {
            return;
        }

        cir.setReturnValue(33);
    }

    @Redirect(method = "createAttributes", at = @At(value = "INVOKE",target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object honeyflavoured$changeArmorPoints( Map<ArmorType, Integer> defense, Object type, Object defaultValue) {
        
        if (this.durability == 37) {
            return defense.getOrDefault((ArmorType)type, (Integer)defaultValue);
        }

        return switch ((ArmorType)type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
            default -> (Integer)defaultValue;
        };
    }
}