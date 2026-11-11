package com.honeyflavoured.jukebox.mixin;

import com.honeyflavoured.jukebox.Jukebox;
import com.honeyflavoured.jukebox.JukeboxAccess;
import com.honeyflavoured.jukebox.JukeboxData;
import com.honeyflavoured.jukebox.JukeboxDiscs;
import com.honeyflavoured.jukebox.JukeboxMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin implements JukeboxAccess {
    @Shadow @Final private JukeboxSongPlayer jukeboxSongPlayer;
    @Shadow public abstract ItemStack getTheItem();
    @Shadow public abstract void setTheItem(ItemStack stack);
    @Shadow public abstract ItemStack splitTheItem(int count);

    @Unique private JukeboxDiscs honeyflavoured$library = new JukeboxDiscs();
    @Unique private int honeyflavoured$index = -1;
    @Unique private boolean honeyflavoured$want;
    @Unique private boolean honeyflavoured$powered;
    @Unique private long honeyflavoured$lastChange;

    // ---------- helpers ----------

    @Unique
    private boolean honeyflavoured$on() {
        Level level = ((BlockEntity) (Object) this).getLevel();
        return level instanceof ServerLevel sl && Jukebox.enabled(sl);
    }

    @Unique
    private JukeboxDiscs honeyflavoured$lib() {
        honeyflavoured$library.setOwner((BlockEntity) (Object) this);
        return honeyflavoured$library;
    }

    @Unique
    private boolean honeyflavoured$playable(int slot) {
        ItemStack stack = honeyflavoured$lib().getItem(slot);
        return !stack.isEmpty() && JukeboxSong.fromStack(stack).isPresent();
    }

    @Unique
    private int honeyflavoured$find(int from, int dir) {
        for (int i = from; i >= 0 && i < JukeboxMenu.DISC_SLOTS; i += dir) {
            if (honeyflavoured$playable(i)) {
                return i;
            }
        }
        return -1;
    }

    @Unique
    private void honeyflavoured$playIndex(ServerLevel level, int slot) {
        honeyflavoured$index = slot;
        honeyflavoured$want = true;
        honeyflavoured$lastChange = level.getGameTime();
        this.setTheItem(honeyflavoured$lib().getItem(slot).copy()); // sets HAS_RECORD and starts the song
        ((BlockEntity) (Object) this).setChanged();
    }

    @Unique
    private void honeyflavoured$pause(ServerLevel level) {
        honeyflavoured$want = false;
        honeyflavoured$lastChange = level.getGameTime();
        BlockEntity self = (BlockEntity) (Object) this;
        jukeboxSongPlayer.stop(level, self.getBlockState());
        self.setChanged();
    }

    @Unique
    private void honeyflavoured$resume(ServerLevel level) {
        int slot = (honeyflavoured$index >= 0 && honeyflavoured$playable(honeyflavoured$index))
                ? honeyflavoured$index : honeyflavoured$find(0, 1);
        if (slot >= 0) {
            honeyflavoured$playIndex(level, slot);
        }
    }

    @Unique
    private void honeyflavoured$toggle(ServerLevel level) {
        if (honeyflavoured$want || jukeboxSongPlayer.isPlaying()) {
            honeyflavoured$pause(level);
        } else {
            honeyflavoured$resume(level);
        }
    }

    @Unique
    private void honeyflavoured$step(ServerLevel level, int dir) {
        int from = honeyflavoured$index < 0 ? (dir > 0 ? 0 : -1) : honeyflavoured$index + dir;
        int slot = honeyflavoured$find(from, dir);
        if (slot >= 0) {
            honeyflavoured$playIndex(level, slot);
        }
    }

    // ---------- JukeboxAccess ----------

    @Override public Container honeyflavoured$discs() { return honeyflavoured$lib(); }
    @Override public ContainerData honeyflavoured$data() { return new JukeboxData(this); }
    @Override public BlockEntity honeyflavoured$entity() { return (BlockEntity) (Object) this; }
    @Override public int honeyflavoured$index() { return honeyflavoured$index; }
    @Override public boolean honeyflavoured$playing() { return honeyflavoured$want || jukeboxSongPlayer.isPlaying(); }

    @Override
    public ItemStack honeyflavoured$displayDisc() {
        if (!honeyflavoured$playing() || honeyflavoured$index < 0) {
            return ItemStack.EMPTY;
        }
        return honeyflavoured$lib().getItem(honeyflavoured$index).copy();
    }

    @Override
    public void honeyflavoured$press(int button) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (!(level instanceof ServerLevel sl) || !Jukebox.enabled(sl)) {
            return;
        }
        switch (button) {
            case JukeboxMenu.BUTTON_PREVIOUS -> honeyflavoured$step(sl, -1);
            case JukeboxMenu.BUTTON_PLAY -> honeyflavoured$toggle(sl);
            case JukeboxMenu.BUTTON_NEXT -> honeyflavoured$step(sl, 1);
            default -> { }
        }
    }

    @Override
    public void honeyflavoured$serverTick(ServerLevel level) {
        BlockEntity self = (BlockEntity) (Object) this;
        JukeboxDiscs lib = honeyflavoured$lib();

        // a disc put in by vanilla before the rule was on becomes slot 0
        if (lib.isEmpty() && honeyflavoured$index < 0 && !this.getTheItem().isEmpty()) {
            lib.setItem(0, this.getTheItem().copy());
            honeyflavoured$index = 0;
            honeyflavoured$want = jukeboxSongPlayer.isPlaying();
        }

        // redstone works as a switch: each rising edge toggles play/pause
        boolean powered = level.hasNeighborSignal(self.getBlockPos());
        if (powered && !honeyflavoured$powered && level.getGameTime() - honeyflavoured$lastChange > 5L) {
            honeyflavoured$toggle(level);
        }
        honeyflavoured$powered = powered;

        // the disc being played was taken out of the chest: stop
        if (!this.getTheItem().isEmpty()) {
            ItemStack inLibrary = honeyflavoured$index >= 0 ? lib.getItem(honeyflavoured$index) : ItemStack.EMPTY;
            if (inLibrary.isEmpty()) {
                honeyflavoured$want = false;
                this.setTheItem(ItemStack.EMPTY);
                return;
            }
        }

        // song ended: go to the next disc to the right / next row, or stop at the end
        if (honeyflavoured$want && !jukeboxSongPlayer.isPlaying()) {
            int next = honeyflavoured$find(honeyflavoured$index + 1, 1);
            if (next >= 0) {
                honeyflavoured$playIndex(level, next);
            } else {
                honeyflavoured$want = false;
                self.setChanged();
            }
        }
    }

    // ---------- container seen by hoppers (36 slots while the rule is on) ----------

    public int getContainerSize() {
        return honeyflavoured$on() ? JukeboxMenu.DISC_SLOTS : 1;
    }

    public boolean isEmpty() {
        return honeyflavoured$on() ? honeyflavoured$lib().isEmpty() : this.getTheItem().isEmpty();
    }

    public ItemStack getItem(int slot) {
        if (honeyflavoured$on()) {
            return honeyflavoured$lib().getItem(slot);
        }
        return slot == 0 ? this.getTheItem() : ItemStack.EMPTY;
    }

    public ItemStack removeItem(int slot, int count) {
        if (honeyflavoured$on()) {
            return honeyflavoured$lib().removeItem(slot, count);
        }
        return slot == 0 ? this.splitTheItem(count) : ItemStack.EMPTY;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        if (honeyflavoured$on()) {
            return honeyflavoured$lib().removeItemNoUpdate(slot);
        }
        return slot == 0 ? this.splitTheItem(1) : ItemStack.EMPTY;
    }

    public void setItem(int slot, ItemStack stack) {
        if (honeyflavoured$on()) {
            honeyflavoured$lib().setItem(slot, stack);
        } else if (slot == 0) {
            this.setTheItem(stack);
        }
    }

    public void clearContent() {
        if (honeyflavoured$on()) {
            honeyflavoured$lib().clearContent();
        } else {
            this.setTheItem(ItemStack.EMPTY);
        }
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void honeyflavoured$canPlace(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (honeyflavoured$on()) {
            cir.setReturnValue(stack.has(DataComponents.JUKEBOX_PLAYABLE)
                    && honeyflavoured$lib().getItem(slot).isEmpty());
        }
    }

    @Inject(method = "canTakeItem", at = @At("HEAD"), cancellable = true)
    private void honeyflavoured$canTake(Container into, int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (honeyflavoured$on()) {
            // hoppers only take discs out once the music has finished
            cir.setReturnValue(!jukeboxSongPlayer.isPlaying() && into.hasAnyMatching(ItemStack::isEmpty));
        }
    }

    // ---------- breaking / rule off: never lose or duplicate discs ----------

    @Inject(method = "popOutTheItem", at = @At("HEAD"), cancellable = true)
    private void honeyflavoured$popAll(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        JukeboxDiscs lib = honeyflavoured$lib();
        if (!lib.isEmpty()) {
            BlockPos pos = self.getBlockPos();
            Containers.dropContents(level, pos, lib);
            lib.clearContent();
            honeyflavoured$index = -1;
            honeyflavoured$want = false;
            this.setTheItem(ItemStack.EMPTY); // the playing copy is only a mirror, it must not drop
            ci.cancel();
        }
    }

    // ---------- tick ----------

    @Inject(method = "tick", at = @At("TAIL"))
    private static void honeyflavoured$afterTick(Level level, BlockPos pos, BlockState state,
                                                 JukeboxBlockEntity jukebox, CallbackInfo ci) {
        if (level instanceof ServerLevel sl && Jukebox.enabled(sl)
                && (Object) jukebox instanceof JukeboxAccess access) {
            access.honeyflavoured$serverTick(sl);
        }
    }

    // ---------- saving ----------

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void honeyflavoured$save(ValueOutput output, CallbackInfo ci) {
        ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list("HoneyDiscs", ItemStackWithSlot.CODEC);
        JukeboxDiscs lib = honeyflavoured$lib();
        for (int i = 0; i < lib.getContainerSize(); i++) {
            ItemStack stack = lib.getItem(i);
            if (!stack.isEmpty()) {
                list.add(new ItemStackWithSlot(i, stack));
            }
        }
        output.putInt("HoneyIndex", honeyflavoured$index);
        output.putBoolean("HoneyPlaying", honeyflavoured$want);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void honeyflavoured$load(ValueInput input, CallbackInfo ci) {
        JukeboxDiscs lib = honeyflavoured$lib();
        lib.clearContent();
        for (ItemStackWithSlot item : input.listOrEmpty("HoneyDiscs", ItemStackWithSlot.CODEC)) {
            if (item.isValidInContainer(JukeboxMenu.DISC_SLOTS)) {
                lib.setItem(item.slot(), item.stack());
            }
        }
        honeyflavoured$index = input.getIntOr("HoneyIndex", -1);
        honeyflavoured$want = input.getBooleanOr("HoneyPlaying", false);
    }
}