package com.honeyflavoured.jukebox.mixin;

import com.honeyflavoured.jukebox.Jukebox;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlock.class)
public abstract class JukeboxBlockMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true)
    private <T extends BlockEntity> void honeyflavoured$ticker(Level level, BlockState state, BlockEntityType<T> type,
                                                               CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        if (level instanceof ServerLevel sl && Jukebox.enabled(sl) && (Object) type == BlockEntityTypes.JUKEBOX) {
            BlockEntityTicker<JukeboxBlockEntity> ticker = JukeboxBlockEntity::tick;
            cir.setReturnValue((BlockEntityTicker<T>) (BlockEntityTicker<?>) ticker);
        }
    }
}