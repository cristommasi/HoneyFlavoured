package com.honeyflavoured.horses;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.honeyflavoured.horses.mixin.MobAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class HorseCall {

	public static final SimpleParticleType TAME_PARTICLE = FabricParticleTypes.simple();

	private static final String KEY_UUID = "honeyflavoured_horse";
	private static final String KEY_NAME = "honeyflavoured_horse_name";

	private static final double TELEPORT_DISTANCE = 100.0D;
	private static final double TELEPORT_HEIGHT = 16.0D;
	private static final double SPAWN_MIN = 8.0D;
	private static final double SPAWN_MAX = 12.0D;
	private static final double ARRIVE_DISTANCE = 4.0D;
	private static final double STEP_DISTANCE = 12.0D;
	private static final double RUN_SPEED = 3.0D;
	private static final int RUN_TIMEOUT = 600;
	private static final int LOAD_TIMEOUT = 100;
	private static final int MAX_FAILURES = 5;
	private static final int SAVE_INTERVAL = 6000;
    private static final double DROP_HEIGHT = 3.0D;
    private static final int STUCK_TICKS = 40;

	private static final Map<UUID, Last> LAST_SEEN = new HashMap<>();
	private static final List<Task> TASKS = new ArrayList<>();

	private static boolean dirty;

	private HorseCall() {}

	private record Last(String dimension, int x, int y, int z) {}

	private record Saved(int priority, Goal goal) {}

	private static final class Task {

		private final UUID horse;
		private final UUID player;
		private final ServerLevel level;
		private final String name;
		private final int chunkX;
		private final int chunkZ;

		private boolean forced;
		private boolean loading;
		private int ticks;
		private int failures;
		private List<Saved> saved;
        private int arrivedTicks;
        private double best = Double.MAX_VALUE;
        private int stuck;

		private Task(UUID horse, UUID player, ServerLevel level, String name, int chunkX, int chunkZ, boolean loading) {

			this.horse = horse;
			this.player = player;
			this.level = level;
			this.name = name;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			this.loading = loading;
			this.forced = loading;
		}
	}

	public static void register() {

		Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("honeyflavoured", "tame"), TAME_PARTICLE);
		UseEntityCallback.EVENT.register(HorseCall::onUseEntity);
		UseItemCallback.EVENT.register(HorseCall::onUseItem);
		ServerTickEvents.END_SERVER_TICK.register(HorseCall::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(HorseCall::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {

			for (Task task : TASKS) {

				release(task);
			}
			TASKS.clear();
			save(server);
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> remember(entity, level));
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {

			if (entity instanceof AbstractHorse horse && !horse.isAlive()) {

				LAST_SEEN.remove(horse.getUUID());
				dirty = true;
				return;
			}
			remember(entity, level);
		});
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {

		ItemStack stack = player.getItemInHand(hand);
		EntityReference<LivingEntity> ownerRef;

		if (!stack.is(Items.GOAT_HORN) || !(entity instanceof AbstractHorse horse) || player.isSpectator() || !horse.isTamed()) {

			return (InteractionResult.PASS);
		}
		ownerRef = horse.getOwnerReference();
		if (ownerRef == null || !ownerRef.getUUID().equals(player.getUUID())) {

			return (InteractionResult.PASS);
		}
		if (level.isClientSide()) {

			return (InteractionResult.SUCCESS);
		}
		if (!horse.hasCustomName()) {

			((ServerPlayer) player).sendSystemMessage(Component.literal("Name your horse with a name tag first").withStyle(ChatFormatting.WHITE), true);
			return (InteractionResult.SUCCESS);
		}
		bind((ServerLevel) level, horse, stack);
		return (InteractionResult.SUCCESS);
	}

	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {

		ItemStack stack = player.getItemInHand(hand);

		if (level.isClientSide() || !stack.is(Items.GOAT_HORN)) {

			return (InteractionResult.PASS);
		}
		call((ServerPlayer) player, (ServerLevel) level, stack);
		return (InteractionResult.PASS);
	}

	private static void bind(ServerLevel level, AbstractHorse horse, ItemStack stack) {

		Component name = horse.getCustomName();

		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {

			tag.putString(KEY_UUID, horse.getStringUUID());
			tag.putString(KEY_NAME, name.getString());
		});
		stack.set(DataComponents.ITEM_NAME, name.copy().withStyle(ChatFormatting.YELLOW));
		remember(horse, level);
		level.sendParticles(TAME_PARTICLE, horse.getX(), horse.getY() + horse.getBbHeight() * 0.8D, horse.getZ(), 7, 0.5D, 0.4D, 0.5D, 0.02D);
	}

	private static void call(ServerPlayer player, ServerLevel level, ItemStack stack) {

		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag;
		UUID id;
		String name;
		Last last;

		if (data == null) {

			return;
		}
		tag = data.copyTag();
		name = tag.getString(KEY_NAME).orElse("Your horse");
		id = parseUuid(tag.getString(KEY_UUID).orElse(""));
		if (id == null) {

			return;
		}
		if (isUnderground(level, player)) {

			deaf(player, name);
			return;
		}
		for (ServerLevel other : level.getServer().getAllLevels()) {

			Entity found = other.getEntity(id);

			if (found instanceof AbstractHorse horse) {

				if (other != level) {

					deaf(player, name);
					return;
				}
				begin(player, level, horse, name);
				return;
			}
		}
		last = LAST_SEEN.get(id);
		if (last == null || !last.dimension().equals(level.dimension().toString())) {

			deaf(player, name);
			return;
		}
		cancelFor(id);
		level.setChunkForced(last.x() >> 4, last.z() >> 4, true);
		TASKS.add(new Task(id, player.getUUID(), level, name, last.x() >> 4, last.z() >> 4, true));
	}

	private static void begin(ServerPlayer player, ServerLevel level, AbstractHorse horse, String name) {

		Task task = new Task(horse.getUUID(), player.getUUID(), level, name, 0, 0, false);

		cancelFor(horse.getUUID());
		if (launch(player, task, horse)) {

			TASKS.add(task);
		}
	}

	private static boolean tooFar(ServerPlayer player, AbstractHorse horse) {

		double dx = player.getX() - horse.getX();
		double dz = player.getZ() - horse.getZ();

		return (Math.sqrt(dx * dx + dz * dz) > TELEPORT_DISTANCE || Math.abs(player.getY() - horse.getY()) > TELEPORT_HEIGHT);
	}

	private static boolean launch(ServerPlayer player, Task task, AbstractHorse horse) {

		if (horse.isVehicle() || !horse.isAlive()) {

			return (false);
		}
		if (tooFar(player, horse)) {

			if (!teleportNear(task.level, player, horse, SPAWN_MIN, SPAWN_MAX)) {

				player.sendSystemMessage(Component.literal(task.name + " couldn't reach you").withStyle(ChatFormatting.WHITE), true);
				return (false);
			}
		}
		return (true);
	}

	private static void tick(MinecraftServer server) {

		Iterator<Task> it = TASKS.iterator();

		if (dirty && server.getTickCount() % SAVE_INTERVAL == 0) {

			save(server);
		}
		while (it.hasNext()) {

			Task task = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(task.player);
			Entity entity = task.level.getEntity(task.horse);
			AbstractHorse horse = (entity instanceof AbstractHorse found) ? found : null;

			task.ticks++;
			if (player == null || player.level() != task.level) {

				release(task);
				it.remove();
				continue;
			}
			if (task.loading) {

				if (horse != null) {

					release(task);
					task.loading = false;
					task.ticks = 0;
					if (!launch(player, task, horse)) {

						it.remove();
					}
				} else if (task.ticks > LOAD_TIMEOUT) {

					release(task);
					LAST_SEEN.remove(task.horse);
					deaf(player, task.name);
					it.remove();
				}
				continue;
			}
			if (!run(player, task, horse)) {

				release(task);
				it.remove();
			}
		}
	}
    
    private static boolean run(ServerPlayer player, Task task, AbstractHorse horse) {

        double distance;

        if (horse == null || !horse.isAlive() || horse.isVehicle()) {

            return (false);
        }
        suppress(task, horse);
        distance = horse.distanceTo(player);
        if (task.arrivedTicks > 0 || distance < ARRIVE_DISTANCE) {

            horse.getNavigation().stop();
            task.arrivedTicks++;
            return (task.arrivedTicks < 60);
        }
        if (distance < task.best - 0.5D) {

            task.best = distance;
            task.stuck = 0;
        } else {

            task.stuck++;
        }
        if (player.getY() - horse.getY() > TELEPORT_HEIGHT || horse.getY() - player.getY() > DROP_HEIGHT || task.stuck > STUCK_TICKS || task.ticks > RUN_TIMEOUT || task.failures >= MAX_FAILURES) {

            teleportNear(task.level, player, horse, 3.0D, 5.0D);
            return (false);
        }
        if (task.ticks % 20 != 1) {

            return (true);
        }
        if (pathToward(task.level, horse, player)) {

            task.failures = 0;
        } else {

            task.failures++;
        }
        return (true);
    }

	private static void suppress(Task task, AbstractHorse horse) {

		GoalSelector selector = ((MobAccessor) horse).honeyflavoured$getGoalSelector();

		if (task.saved != null) {

			return;
		}
		task.saved = new ArrayList<>();
		for (WrappedGoal wrapped : new ArrayList<>(selector.getAvailableGoals())) {

			if (!(wrapped.getGoal() instanceof FloatGoal)) {

				task.saved.add(new Saved(wrapped.getPriority(), wrapped.getGoal()));
			}
		}
		for (Saved goal : task.saved) {

			selector.removeGoal(goal.goal());
		}
		horse.clearStanding();
		horse.setEating(false);
	}

	private static boolean pathToward(ServerLevel level, AbstractHorse horse, ServerPlayer player) {

		double dx = player.getX() - horse.getX();
		double dz = player.getZ() - horse.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		int x;
		int z;
		int y;

		if (horse.distanceTo(player) <= STEP_DISTANCE || length < 1.0D) {

			return (horse.getNavigation().moveTo(player, RUN_SPEED));
		}
		x = Mth.floor(horse.getX() + dx / length * STEP_DISTANCE);
		z = Mth.floor(horse.getZ() + dz / length * STEP_DISTANCE);
		y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return (horse.getNavigation().moveTo(x + 0.5D, y, z + 0.5D, RUN_SPEED));
	}

	private static boolean teleportNear(ServerLevel level, ServerPlayer player, AbstractHorse horse, double minRadius, double maxRadius) {

		BlockPos origin = player.blockPosition();
		RandomSource random = level.getRandom();

		for (int attempt = 0; attempt < 30; attempt++) {

			double angle = random.nextDouble() * Math.PI * 2.0D;
			double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
			int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
			int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);

			for (int i = 0; i <= 16; i++) {

				int dy = ((i & 1) == 0) ? (i / 2) : -((i + 1) / 2);
				BlockPos pos = new BlockPos(x, origin.getY() + dy, z);

				if (isSafe(level, horse, pos)) {

					horse.getNavigation().stop();
					horse.setDeltaMovement(Vec3.ZERO);
					horse.snapTo(x + 0.5D, pos.getY(), z + 0.5D, horse.getYRot(), horse.getXRot());
					horse.resetFallDistance();
					level.playSound(null, horse.getX(), horse.getY(), horse.getZ(), SoundEvents.HORSE_ANGRY_BABY, SoundSource.NEUTRAL, 1.0F, 1.0F);
					return (true);
				}
			}
		}
		return (false);
	}

	private static boolean isSafe(ServerLevel level, AbstractHorse horse, BlockPos pos) {

		BlockPos below = pos.below();
		AABB box;

		if (!level.isLoaded(pos) || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {

			return (false);
		}
		box = horse.getDimensions(horse.getPose()).makeBoundingBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		return (level.noCollision(horse, box) && !level.containsAnyLiquid(box));
	}

	private static boolean isUnderground(ServerLevel level, ServerPlayer player) {

		BlockPos feet = player.blockPosition();
		BlockPos eyes = BlockPos.containing(player.getEyePosition());

		if (level.getBlockState(feet).is(Blocks.CAVE_AIR) || level.getBlockState(eyes).is(Blocks.CAVE_AIR)) {

			return (true);
		}
		if (!level.dimensionType().hasSkyLight()) {

			return (false);
		}
		return (!level.canSeeSky(eyes) && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX(), feet.getZ()) - feet.getY() > 8);
	}

	private static void deaf(ServerPlayer player, String name) {

		player.sendSystemMessage(Component.literal(name + " couldn't hear you").withStyle(ChatFormatting.WHITE), true);
	}

	private static void cancelFor(UUID horse) {

		Iterator<Task> it = TASKS.iterator();

		while (it.hasNext()) {

			Task task = it.next();

			if (task.horse.equals(horse)) {

				release(task);
				it.remove();
			}
		}
	}

	private static void release(Task task) {

		if (task.forced) {

			task.level.setChunkForced(task.chunkX, task.chunkZ, false);
			task.forced = false;
		}
		if (task.saved != null) {

			if (task.level.getEntity(task.horse) instanceof AbstractHorse horse) {

				GoalSelector selector = ((MobAccessor) horse).honeyflavoured$getGoalSelector();

				for (Saved goal : task.saved) {

					selector.addGoal(goal.priority(), goal.goal());
				}
			}
			task.saved = null;
		}
	}

	private static UUID parseUuid(String text) {

		try {

			return (UUID.fromString(text));
		} catch (IllegalArgumentException e) {

			return (null);
		}
	}

	private static void remember(Entity entity, ServerLevel level) {

		if (!(entity instanceof AbstractHorse horse) || !horse.isTamed()) {

			return;
		}
		LAST_SEEN.put(horse.getUUID(), new Last(level.dimension().toString(), horse.getBlockX(), horse.getBlockY(), horse.getBlockZ()));
		dirty = true;
	}

	private static Path file(MinecraftServer server) {

		return (server.getWorldPath(LevelResource.ROOT).resolve("honeyflavoured_horses.txt"));
	}

	private static void load(MinecraftServer server) {

		Path path = file(server);

		LAST_SEEN.clear();
		if (!Files.exists(path)) {

			return;
		}
		try {

			for (String line : Files.readAllLines(path)) {

				String[] part = line.split("\t");

				if (part.length == 5) {

					LAST_SEEN.put(UUID.fromString(part[0]), new Last(part[1], Integer.parseInt(part[2]), Integer.parseInt(part[3]), Integer.parseInt(part[4])));
				}
			}
		} catch (IOException | IllegalArgumentException e) {

			LAST_SEEN.clear();
		}
	}

	private static void save(MinecraftServer server) {

		List<String> lines = new ArrayList<>();

		for (Map.Entry<UUID, Last> entry : LAST_SEEN.entrySet()) {

			Last last = entry.getValue();

			lines.add(entry.getKey() + "\t" + last.dimension() + "\t" + last.x() + "\t" + last.y() + "\t" + last.z());
		}
		try {

			Files.write(file(server), lines);
			dirty = false;
		} catch (IOException e) {

			dirty = true;
		}
	}
}