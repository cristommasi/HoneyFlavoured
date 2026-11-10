
package com.honeyflavoured.mounts.mixin;

import com.honeyflavoured.mounts.MountInventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryMenu.class)
public abstract class MountInventoryMenuMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static int honeyflavoured$skipVanillaGrid(int columns) {

        return columns > 0 ? 0 : columns;
    }


    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;addStandardInventorySlots(Lnet/minecraft/world/Container;II)V"
        )
    )
    private void honeyflavoured$addMountGrid(int containerId, Inventory playerInventory, Container horseInventory, AbstractHorse horse, int inventoryColumns, CallbackInfo ci) {

        for (int slot = 0; slot < horseInventory.getContainerSize(); slot++) {

            int x = slot % MountInventory.COLUMNS;
            int y = slot / MountInventory.COLUMNS;

            ((AbstractContainerMenuAccessor) this).honeyflavoured$addSlot(
                new Slot(
                    horseInventory,
                    slot,
                    MountInventory.GRID_X + x * 18,
                    MountInventory.GRID_Y + y * 18
                )
            );
        }
    }
}
