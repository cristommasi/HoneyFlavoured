package com.honeyflavoured.farming.mixin;

import com.honeyflavoured.commands.HoneyFlavouredCommands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class FarmingMixin {

    // Relative crop breaking progress. Higher means faster.
    private static final float NO_HOE_SPEED = 0.050F;
    private static final float WOODEN_HOE_SPEED = 0.15F;
    private static final float STONE_HOE_SPEED = 0.25F;
    private static final float COPPER_HOE_SPEED = 0.35F;
    private static final float IRON_HOE_SPEED = 0.45F;

    private static final boolean GOLD_INSTANT = true;
    private static final boolean DIAMOND_INSTANT = true;
    private static final boolean NETHERITE_INSTANT = true;

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true, require = 1)
    private void honeyflavoured$changeCropBreakingSpeed(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {

        // Gamerule honeyflavoured:farming_changes false = vanilla crop breaking.
        if (!HoneyFlavouredCommands.enabled(player.level(), HoneyFlavouredCommands.USE_SLOW_FARMING_SPEED)) {
            return;
        }

        BlockState state = (BlockState) (Object) this;
        Block block = state.getBlock();

        boolean crop = block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES || block == Blocks.BEETROOTS;

        if (!crop) {
            return;
        }

        var tool = player.getMainHandItem();

        if ((GOLD_INSTANT && tool.is(Items.GOLDEN_HOE)) || (DIAMOND_INSTANT && tool.is(Items.DIAMOND_HOE)) || (NETHERITE_INSTANT && tool.is(Items.NETHERITE_HOE))) {
            cir.setReturnValue(1.0F);
            return;
        }

        int tier;

        if (tool.is(Items.WOODEN_HOE)) {
            tier = 1;
        }
        else if (tool.is(Items.STONE_HOE)) {
            tier = 2;
        }
        else if (tool.is(Items.COPPER_HOE)) {
            tier = 3;
        }
        else if (tool.is(Items.IRON_HOE)) {
            tier = 4;
        }
        else {
            tier = 0;
        }

        var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var efficiency = enchantments.getOrThrow(Enchantments.EFFICIENCY);
        int efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiency, tool);

        int effectiveTier = tier + efficiencyLevel;

        if (effectiveTier >= 5) {
            cir.setReturnValue(1.0F);
            return;
        }

        float speed;

        if (effectiveTier == 4) {
            speed = IRON_HOE_SPEED;
        }
        else if (effectiveTier == 3) {
            speed = COPPER_HOE_SPEED;
        }
        else if (effectiveTier == 2) {
            speed = STONE_HOE_SPEED;
        }
        else if (effectiveTier == 1) {
            speed = WOODEN_HOE_SPEED;
        }
        else {
            speed = NO_HOE_SPEED;
        }

        cir.setReturnValue(speed);
    }
}