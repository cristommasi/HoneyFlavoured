package com.honeyflavoured.temperature;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;


public final class TemperatureManager {

    private TemperatureManager() {}

    public enum TemperatureType { NORMAL, COLD, HOT }

    private static final Map<Player, TemperatureData> DATA = new WeakHashMap<>();

    /* Biomes that make you cold. The whole End dimension counts too. */
    private static final Set<ResourceKey<Biome>> COLD_BIOMES = Set.of(
        Biomes.THE_END, Biomes.SMALL_END_ISLANDS, Biomes.END_MIDLANDS, Biomes.END_HIGHLANDS, Biomes.END_BARRENS,
        Biomes.ICE_SPIKES, Biomes.SNOWY_PLAINS, Biomes.SNOWY_BEACH, Biomes.FROZEN_RIVER, Biomes.RIVER,
        Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES, Biomes.GROVE, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS,
        Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_OCEAN
    );

    /* Biomes that make you hot. The whole Nether dimension counts too. */
    private static final Set<ResourceKey<Biome>> HOT_BIOMES = Set.of(
        Biomes.DESERT, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS,
        Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST, Biomes.BASALT_DELTAS
    );

    private static final class TemperatureData {
        private float coldExposure;
        private float hotExposure;
        private float lastHealth = -1.0F;
        private float sentCold = -1.0F;
        private float sentHot = -1.0F;
        private int damageTimer;
        private int heatEffectTicks;
        private int coolingEffectTicks;
        private boolean damageWasReady;
        private int coldEnterTicks;
        private int hotEnterTicks;
    }

    public static void register() {
        TemperatureAttributes.register();
        TemperatureArmor.register();

        PayloadTypeRegistry.clientboundPlay().register(TemperaturePayload.TYPE, TemperaturePayload.CODEC);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });

        // Fire Aspect sword hits and flaming arrow hits help warm the player.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer player) || blocked || !player.isOnFire()) return;

            if (source.getDirectEntity() instanceof Player || source.getDirectEntity() instanceof AbstractArrow) {
                applyHeatEffect(player);
            }
        });
    }

    public static void tickPlayer(ServerPlayer player) {
        TemperatureData data = DATA.computeIfAbsent(player, p -> new TemperatureData());

        if (data.heatEffectTicks > 0) data.heatEffectTicks--;
        if (data.coolingEffectTicks > 0) data.coolingEffectTicks--;

        if (player.isSpectator() || player.isCreative()) {
            data.coldExposure = 0.0F;
            data.hotExposure = 0.0F;
            data.damageTimer = 0;
            data.damageWasReady = false;
            data.coldEnterTicks = 0;
            data.hotEnterTicks = 0;
            data.lastHealth = player.getHealth();
            sync(player, data);
            return;
        }

        boolean nearEndPortal = isNearBlock(player, Blocks.END_PORTAL);
        boolean nearNetherPortal = isNearBlock(player, Blocks.NETHER_PORTAL);
        boolean coldEnvironment = isColdEnvironment(player, nearEndPortal);
        boolean hotEnvironment = isHotEnvironment(player, nearEndPortal, nearNetherPortal);
        float coldProtection = protection(player, TemperatureAttributes.CALEFACTION);

        // Entering a cold / hot area does nothing until you have stayed there for a while.
        int delay = TemperatureConfig.ENVIRONMENT_ENTRY_DELAY_TICKS;
        data.coldEnterTicks = coldEnvironment ? Math.min(data.coldEnterTicks + 1, delay + 1) : 0;
        data.hotEnterTicks = hotEnvironment ? Math.min(data.hotEnterTicks + 1, delay + 1) : 0;
        boolean coldActive = coldEnvironment && data.coldEnterTicks > delay;
        boolean hotActive = hotEnvironment && data.hotEnterTicks > delay;

        data.coldExposure = updateExposure(
            data.coldExposure, 
            coldActive,
            coldProtection, 
            isNearHeatSource(player) || nearEndPortal || nearNetherPortal || data.heatEffectTicks > 0, 
            TemperatureConfig.COLD_BUILDUP_TICKS, TemperatureConfig.COLD_RECOVERY_TICKS);

        data.hotExposure = updateExposure(
            data.hotExposure,
            hotActive,
            protection(player, TemperatureAttributes.SWELTERING),
            isNearColdSource(player) || data.coolingEffectTicks > 0,
            TemperatureConfig.HOT_BUILDUP_TICKS,
            TemperatureConfig.HOT_RECOVERY_TICKS
        );

        // Cold water instantly freezes the player unless Calefaction provides immunity.
        if (coldActive && player.isInWater() && coldProtection < 1.0F) data.coldExposure = 1.0F;

        // Powder snow does the same, in any biome, and ignores the entry delay like vanilla does.
        if (player.isInPowderSnow && coldProtection < 1.0F) data.coldExposure = 1.0F;

        // Being in lava instantly heats all hearts.
        if (player.isInLava()) data.hotExposure = 1.0F;


        // Count coloured hearts against maximum health, not current health.
        int maxHearts = Mth.ceil(player.getMaxHealth() / 2.0F);
        int currentHearts = Mth.ceil(player.getHealth() / 2.0F);
        int coldHearts = Mth.ceil(maxHearts * data.coldExposure);
        int hotHearts = Mth.ceil(maxHearts * data.hotExposure);

        boolean coldDamageReady = player.getHealth() > 0.0F && coldHearts >= currentHearts;
        boolean hotDamageReady = player.getHealth() > 0.0F && hotHearts >= currentHearts;
        boolean damageReady = coldDamageReady || hotDamageReady;

        // Deal the first damage immediately when coloured hearts reach current health.
        int damageInterval = Math.max(1, TemperatureConfig.DAMAGE_INTERVAL_TICKS);

        if (damageReady && !data.damageWasReady) data.damageTimer = damageInterval - 1;


        if (damageReady) {
            if (data.lastHealth >= 0.0F && player.getHealth() > data.lastHealth) player.setHealth(data.lastHealth);

            if (++data.damageTimer >= damageInterval) {
                data.damageTimer = 0;

                if (player.getHealth() > 0.0F) {
                    float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
                    player.level().playSound(null, player.blockPosition(), hotDamageReady ? SoundEvents.PLAYER_HURT_ON_FIRE : SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 2.0F, pitch);
                    DamageSource damageSource = player.damageSources().generic();
                    player.hurtServer((ServerLevel) player.level(), damageSource, 1.0F);
                }
            }
        }
        else {
            data.damageTimer = 0;
        }

        data.damageWasReady = damageReady;
        data.lastHealth = player.getHealth();

        sync(player, data);
    }

    private static float updateExposure(float exposure, boolean inEnvironment, float protection, boolean helped, int buildTicks, int recoveryTicks) {
        if (!inEnvironment) return Math.max(0.0F, exposure - 1.0F / Math.max(1, recoveryTicks));
        if (protection >= 1.0F) return Math.max(0.0F, exposure - recoveryRate(recoveryTicks, TemperatureConfig.PROTECTION_RECOVERY_MULTIPLIER));
        if (helped) return Math.max(0.0F, exposure - recoveryRate(recoveryTicks, TemperatureConfig.ENVIRONMENT_RECOVERY_MULTIPLIER));

        return Math.min(1.0F, exposure + (1.0F - protection) / Math.max(1, buildTicks));
    }

    private static float protection(Player player, Holder<Attribute> attribute) {
        return (float) Mth.clamp(player.getAttributeValue(attribute) / TemperatureConfig.RESISTANCE_FOR_IMMUNITY, 0.0D, 1.0D);
    }

    private static float recoveryRate(int ticks, float multiplier) {
        return multiplier / Math.max(1, ticks);
    }

    private static void sync(ServerPlayer player, TemperatureData data) {
        if (changed(data.coldExposure, data.sentCold) || changed(data.hotExposure, data.sentHot)) {
            ServerPlayNetworking.send(player, new TemperaturePayload(data.coldExposure, data.hotExposure));
            data.sentCold = data.coldExposure;
            data.sentHot = data.hotExposure;
        }
    }

    private static boolean changed(float value, float sent) {
        if (sent < 0.0F) return true;
        if (value == sent) return false;
        if (value <= 0.0F || value >= 1.0F) return true;
        return Math.abs(value - sent) >= TemperatureConfig.SYNC_STEP;
    }

    private static boolean inBiome(Player player, Set<ResourceKey<Biome>> biomes) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());

        for (ResourceKey<Biome> key : biomes) {
            if (biome.is(key)) return true;
        }

        return false;
    }

    /* Overworld: cold biomes, or near an End portal (it leads to the End). End: cold everywhere except near the return portal (it leads home). Nether: never cold. */
    private static boolean isColdEnvironment(Player player, boolean nearEndPortal) {
        Level level = player.level();

        if (level.dimension() == Level.NETHER) return false;
        if (level.dimension() == Level.END) return !nearEndPortal;

        return nearEndPortal || inBiome(player, COLD_BIOMES);
    }

    /* Overworld: hot biomes, or near a Nether portal (it leads to the Nether). Nether: hot everywhere except near a Nether portal (it leads home). End: never hot. */
    private static boolean isHotEnvironment(Player player, boolean nearEndPortal, boolean nearNetherPortal) {
        Level level = player.level();

        if (level.dimension() == Level.NETHER) return !nearNetherPortal;
        if (level.dimension() == Level.END) return false;

        return nearNetherPortal || inBiome(player, HOT_BIOMES);
    }

    private static boolean isNearBlock(Player player, Block target) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int range = TemperatureConfig.HEAT_SOURCE_RANGE;

        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
            if (level.getBlockState(nearby).is(target)) return true;
        }

        return false;
    }

    private static boolean isNearHeatSource(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int range = TemperatureConfig.HEAT_SOURCE_RANGE;

        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-range, -range, -range), pos.offset(range, range, range))) {
            Block block = level.getBlockState(nearby).getBlock();

            if (block == Blocks.LAVA || block == Blocks.FIRE || block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_FIRE || block == Blocks.SOUL_WALL_TORCH || block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE || block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN || block == Blocks.MAGMA_BLOCK) return true;
        }

        return false;
    }

    private static boolean isNearColdSource(Player player) {
        if (player.isInWater() || player.isInPowderSnow) return true;
        if (player.isUsingItem() && player.getUseItem().is(Items.POTION)) return true;

        // Standing on, inside, or right next to snow and ice (the layer below the feet counts too).
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 0, 1))) {
            Block block = level.getBlockState(nearby).getBlock();

            if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW || block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE || block == Blocks.FROSTED_ICE) return true;
        }

        return false;
    }

    public static float getColdExposure(Player player) {
        TemperatureData data = DATA.get(player);
        return data == null ? 0.0F : data.coldExposure;
    }

    public static float getHotExposure(Player player) {
        TemperatureData data = DATA.get(player);
        return data == null ? 0.0F : data.hotExposure;
    }

    public static void applyHeatEffect(Player player) {
        TemperatureData data = DATA.computeIfAbsent(player, p -> new TemperatureData());
        data.heatEffectTicks = TemperatureConfig.HIT_EFFECT_DURATION_TICKS;
    }

    public static void applyCoolingEffect(Player player) {
        TemperatureData data = DATA.computeIfAbsent(player, p -> new TemperatureData());
        data.coolingEffectTicks = TemperatureConfig.HIT_EFFECT_DURATION_TICKS;
    }

}