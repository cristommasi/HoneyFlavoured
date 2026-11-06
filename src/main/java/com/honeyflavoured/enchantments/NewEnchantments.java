package com.honeyflavoured.enchantments;

import java.util.function.Predicate;

import com.honeyflavoured.commands.HoneyFlavouredCommands;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.registries.BuiltInRegistries;

public final class NewEnchantments {

    private NewEnchantments() {}

    public static final ResourceKey<Enchantment> TUNNELING = key("tunneling");
    public static final ResourceKey<Enchantment> IGNEOUS_STRIDER = key("igneous_strider");
    public static final ResourceKey<Enchantment> MELEE_PROTECTION = key("melee_protection");
    public static final ResourceKey<Enchantment> SWINENESS = key("swineness");
    public static final ResourceKey<Enchantment> BANE_OF_POLYPODS = key("bane_of_polypods");
    public static final ResourceKey<Enchantment> LAVA_AFFINITY = key("lava_affinity");
    public static final ResourceKey<Enchantment> SERENITY = key("serenity");
    public static final ResourceKey<Enchantment> THAUMATURGY = key("thaumaturgy");

    /** An armour piece can carry at most this many different protection enchantments. */
    public static final int MAX_PROTECTIONS = 2;
    /** A weapon can carry at most this many of: Smite, Swineness, Serenity, Thaumaturgy, Bane of Polypods. */
    public static final int MAX_SPECIALISTS = 2;
    public static final int LAVA_IMMUNITY_LEVEL = 5;
    public static final int RESPIRATION_RESTRICTED_LEVEL = 4;
    public static volatile boolean enabled = true;

    private static final EquipmentSlot[] ALL_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};

    private static ResourceKey<Enchantment> key(String name) {

        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("honeyflavoured", name));
    }

    public static void register() {

        PayloadTypeRegistry.clientboundPlay().register(NewEnchantmentsPayload.TYPE, NewEnchantmentsPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            boolean now = HoneyFlavouredCommands.enabled(server.overworld(), HoneyFlavouredCommands.USE_NEW_ENCHANTMENTS);
            enabled = now;
            ServerPlayNetworking.send(handler.getPlayer(), new NewEnchantmentsPayload(now));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean now = HoneyFlavouredCommands.enabled(server.overworld(), HoneyFlavouredCommands.USE_NEW_ENCHANTMENTS);

            if (now != enabled) {
                enabled = now;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(player, new NewEnchantmentsPayload(now));
                }
            }
        });

        Tunneling.register();
        FireProtection.register();
        BlastProtection.register();
    }

    public static Holder<Enchantment> holder(RegistryAccess access, ResourceKey<Enchantment> key) {
        return access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static boolean isProtection(Holder<Enchantment> h) {
        return h.is(Enchantments.PROTECTION) || h.is(Enchantments.FIRE_PROTECTION)
            || h.is(Enchantments.BLAST_PROTECTION) || h.is(Enchantments.PROJECTILE_PROTECTION)
            || h.is(MELEE_PROTECTION);
    }

    private static boolean isSpecialist(Holder<Enchantment> h) {
        return h.is(Enchantments.SMITE) || h.is(SWINENESS) || h.is(SERENITY)
            || h.is(THAUMATURGY) || h.is(BANE_OF_POLYPODS);
    }

    /**
     * True when adding this enchantment would put one kind too many of this group on the item.
     * Raising an enchantment the item already has is always fine.
     */
    private static boolean tooMany(ItemEnchantments existing, Holder<Enchantment> adding, Predicate<Holder<Enchantment>> kind, int max) {
        if (!kind.test(adding)) {
            return false;
        }

        int others = 0;

        for (Holder<Enchantment> h : existing.keySet()) {
            if (h.equals(adding)) {
                return false;
            }
            if (kind.test(h)) {
                others++;
            }
        }

        return others >= max;
    }

    /** Used by the enchanting menu: would this be a 3rd kind of protection? */
    public static boolean tooManyProtections(ItemEnchantments existing, Holder<Enchantment> adding) {
        return tooMany(existing, adding, NewEnchantments::isProtection, MAX_PROTECTIONS);
    }

    /** Used by the enchanting menu: would this be a 3rd specialist damage enchantment? */
    public static boolean tooManySpecialists(ItemEnchantments existing, Holder<Enchantment> adding) {
        return tooMany(existing, adding, NewEnchantments::isSpecialist, MAX_SPECIALISTS);
    }

    /** Highest level of an enchantment on any worn armour piece. */
    public static int armorLevel(LivingEntity entity, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = holder(entity.level().registryAccess(), key);
        int best = 0;

        for (EquipmentSlot slot : ALL_SLOTS) {
            best = Math.max(best, EnchantmentHelper.getItemEnchantmentLevel(holder, entity.getItemBySlot(slot)));
        }

        return best;
    }

    public static boolean isGoldArmor(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("golden_helmet") || id.equals("golden_chestplate") || id.equals("golden_leggings")
            || id.equals("golden_boots") || id.equals("golden_horse_armor") || id.equals("golden_nautilus_armor");
    }

    /** True if putting this enchantment at this level on this item breaks the "level 5 = gold only" rule. */
    public static boolean fireProtectionVNeedsGold(Holder<Enchantment> adding, int level, ItemStack target) {
        return adding.is(Enchantments.FIRE_PROTECTION)
            && level >= LAVA_IMMUNITY_LEVEL
            && !isGoldArmor(target);
    }

    /** Lava immunity: a golden armour piece worn with Fire Protection V. */
    public static boolean hasLavaImmunity(LivingEntity entity) {
        Holder<Enchantment> fire = holder(entity.level().registryAccess(), Enchantments.FIRE_PROTECTION);

        for (EquipmentSlot slot : ALL_SLOTS) {
            ItemStack piece = entity.getItemBySlot(slot);

            if (isGoldArmor(piece) && EnchantmentHelper.getItemEnchantmentLevel(fire, piece) >= LAVA_IMMUNITY_LEVEL) {
                return true;
            }
        }
        return false;
    }


    public static boolean isRespirationIVHelmet(ItemStack stack) {
        return stack.is(Items.GOLDEN_HELMET) || stack.is(Items.IRON_HELMET) || stack.is(Items.TURTLE_HELMET);
    }

    /** True if putting this enchantment at this level on this item breaks the "Respiration IV = gold/iron/turtle only" rule. */
    public static boolean respirationIVNeedsHelmet(Holder<Enchantment> adding, int level, ItemStack target) {
        return adding.is(Enchantments.RESPIRATION)
            && level >= RESPIRATION_RESTRICTED_LEVEL
            && !isRespirationIVHelmet(target);
    }

    /** True if this finished item breaks one of our rules. Used by the anvil. */
    public static boolean violatesRules(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        ItemEnchantments ench = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int protections = 0;
        int specialists = 0;

        for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
            Holder<Enchantment> h = e.getKey();
            int level = e.getIntValue();

            if (isProtection(h)) {
                protections++;
            }
            if (isSpecialist(h)) {
                specialists++;
            }
            if (fireProtectionVNeedsGold(h, level, stack) || respirationIVNeedsHelmet(h, level, stack)) {
                return true;
            }
        }
        return protections > MAX_PROTECTIONS || specialists > MAX_SPECIALISTS;
    }

    /** Sum of an enchantment's levels over all worn armour pieces. */
    public static int armorLevelSum(LivingEntity entity, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = holder(entity.level().registryAccess(), key);
        int sum = 0;

        for (EquipmentSlot slot : ALL_SLOTS) {
            sum += EnchantmentHelper.getItemEnchantmentLevel(holder, entity.getItemBySlot(slot));
        }
        return sum;
    }

}