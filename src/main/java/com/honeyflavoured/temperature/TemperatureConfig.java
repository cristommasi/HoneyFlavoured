package com.honeyflavoured.temperature;

public final class TemperatureConfig {

    private TemperatureConfig() {}

    // Ticks spent in a dangerous environment with ZERO protection before exposure reaches 100%.
    // Each armor point of resistance scales this up (see RESISTANCE_FOR_IMMUNITY).
    public static final int COLD_BUILDUP_TICKS = 2400 / 2;
    public static final int HOT_BUILDUP_TICKS = 2400 / 2;
    
    // Ticks you must stay in a hot or cold area before exposure starts building (20 ticks = 1 second).
    public static final int ENVIRONMENT_ENTRY_DELAY_TICKS = 200;
    // Time taken to recover one full exposure bar.
    public static final int COLD_RECOVERY_TICKS = 300;
    public static final int HOT_RECOVERY_TICKS = 200;

    // Damage once every heart is temperature-coloured (exposure = 100%).
    // 1.0F = half a heart.
    public static final int DAMAGE_INTERVAL_TICKS = 80;
    public static final float DAMAGE_AMOUNT = 1.0F;

    // Resistance is read from the calefaction / sweltering attributes.
    // Total resistance of this value (or more) makes the player immune.
    // With 4 armor pieces at +1 each: 4 pieces = immune.
    public static final double RESISTANCE_FOR_IMMUNITY = 4.0;

    // Attribute value each armor piece grants.
    public static final double RESISTANCE_PER_ARMOR_PIECE = 1.0;

    // Nearby heat sources that help against cold.
    public static final int HEAT_SOURCE_RANGE = 3;

    // Exposure recovery rate multiplier when fully protected or helped by the environment.
    public static final float PROTECTION_RECOVERY_MULTIPLIER = 2.0F;
    public static final float ENVIRONMENT_RECOVERY_MULTIPLIER = 0.5F;

    // Biome temperature thresholds.
    public static final float COLD_BIOME_TEMPERATURE = 0.15F;
    public static final float HOT_BIOME_TEMPERATURE = 1.5F;

    // Darkness effect applied while exposure is 100%.
    public static final int DARKNESS_DURATION_TICKS = 100;
    public static final int DARKNESS_REFRESH_TICKS = 40;

    // Sound played each time another heart turns cold / hot.
    public static final float SOUND_VOLUME = 0.8F;

    // Minimum exposure change before the server re-syncs to the client.
    public static final float SYNC_STEP = 0.005F;

    // Duration of temporary heat/cooling effects from attacks, in ticks.
    public static final int HIT_EFFECT_DURATION_TICKS = 60;
}