package com.honeyflavoured.enchantments.enchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.honeyflavoured.enchantments.NewEnchantments;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Tunneling (pickaxe, shovel): mining a block also breaks the 8 blocks around it, in the flat 3x3 square
 * facing you. Every block costs durability like normal, so a full 3x3 costs 9. It only breaks blocks the tool
 * is good at (a pickaxe breaks stone but leaves dirt, a shovel the other way round) and never bedrock and the like.
 */
public final class Tunneling {

    private Tunneling() {}

    /** The face of the block each player was mining, remembered just before it breaks. */
    private static final Map<UUID, Direction> FACE = new HashMap<>();

    /** True while we are breaking the extra blocks, so they do not trigger more tunneling. */
    private static boolean breaking;

    public static void register() {


        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (NewEnchantments.enabled && player instanceof ServerPlayer && !breaking && hasTunneling(player.getMainHandItem(), level.registryAccess())) {
                HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);

                if (hit instanceof BlockHitResult block && block.getBlockPos().equals(pos)) {
                    FACE.put(player.getUUID(), block.getDirection());
                }
            }
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            Direction face = FACE.remove(player.getUUID());

            if (face == null || breaking || !(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel serverLevel)) {
                return;
            }

            ItemStack tool = sp.getMainHandItem();

            if (!NewEnchantments.enabled || !hasTunneling(tool, level.registryAccess())) {
                return;
            }

            // The two directions that lie flat in the face we are mining.
            Direction.Axis a;
            Direction.Axis b;

            switch (face.getAxis()) {
                case X -> { a = Direction.Axis.Y; b = Direction.Axis.Z; }
                case Y -> { a = Direction.Axis.X; b = Direction.Axis.Z; }
                default -> { a = Direction.Axis.X; b = Direction.Axis.Y; }
            }

            breaking = true;

            try {
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {

                        if (i == 0 && j == 0) continue;

                        // The tool may have broken part way through.
                        if (sp.getMainHandItem() != tool || tool.isEmpty()) return;

                        BlockPos target = pos.relative(a, i).relative(b, j);
                        BlockState targetState = serverLevel.getBlockState(target);

                        if (targetState.isAir()
                            || targetState.getDestroySpeed(serverLevel, target) < 0.0F
                            || tool.getDestroySpeed(targetState) <= 1.0F
                            || !tool.isCorrectToolForDrops(targetState)
                            || sp.blockActionRestricted(serverLevel, target, sp.gameMode.getGameModeForPlayer())) {
                            continue;
                        }

                        sp.gameMode.destroyBlock(target);
                    }
                }
            }
            finally {
                breaking = false;
            }
        });
    }

    private static boolean hasTunneling(ItemStack tool, net.minecraft.core.RegistryAccess access) {

        return !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(NewEnchantments.holder(access, NewEnchantments.TUNNELING), tool) > 0;
    }
}