/*
 * Copyright (c) 2026 Trowelanderror
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons allowable, subject to the
 * following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package com.trowelanderror.item;

import com.trowelanderror.history.BlockChange;
import com.trowelanderror.history.HistoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class CopyPasteTrowelItem extends BaseTrowelItem {

    private static final int MAX_COPY_VOLUME = 32768;

    public CopyPasteTrowelItem(Properties properties) {
        super(properties);
    }

    // ---------- shared statics (also used by the client preview) ----------

    public static CompoundTag getCustomTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static int dirIndex(Direction d) {
        return switch (d) {
            case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3;
            default -> 0;
        };
    }

    public static Rotation rotationFor(int copyIdx, int pasteIdx) {
        return switch ((pasteIdx - copyIdx + 4) % 4) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static int[] rotateOffset(int x, int z, Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> new int[]{-z, x};
            case CLOCKWISE_180 -> new int[]{-x, -z};
            case COUNTERCLOCKWISE_90 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    // ------------------------------- use ----------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        CompoundTag tag = getCustomTag(stack);

        if (player.isShiftKeyDown()) {
            clearAll(stack);
            player.displayClientMessage(Component.literal("Trowel cleared."), true);
            return InteractionResultHolder.success(stack);
        }

        if (tag.contains("blocks")) {
            ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
            player.displayClientMessage(Component.literal(
                    "Holding " + blocks.size() +
                            " blocks. Click a surface to paste (rotation follows your facing). Sneak-click to clear."), true);
        } else if (tag.contains("pos1_x")) {
            player.displayClientMessage(Component.literal("Point A set. Click the opposite corner to copy."), true);
        } else {
            player.displayClientMessage(Component.literal("Click two corners to copy a region."), true);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        CompoundTag tag = getCustomTag(stack);

        boolean hasCopy = tag.contains("blocks");
        boolean hasPos1 = tag.contains("pos1_x");

        // Sneak-click on a block: abandon current state, start a fresh copy
        if (player.isShiftKeyDown()) {
            clearAll(stack);
            hasCopy = false;
            hasPos1 = false;
        }

        if (!hasCopy && !hasPos1) {
            // --- Click 1: corner A ---
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
                nbt.putInt("pos1_x", clicked.getX());
                nbt.putInt("pos1_y", clicked.getY());
                nbt.putInt("pos1_z", clicked.getZ());
            });
            player.displayClientMessage(Component.literal(
                    "Copy point A set at " + clicked.toShortString() + ". Click the opposite corner."), true);
            return InteractionResult.SUCCESS;
        }

        if (!hasCopy) {
            // --- Click 2: corner B -> snapshot ---
            BlockPos pos1 = new BlockPos(
                    tag.getInt("pos1_x"),
                    tag.getInt("pos1_y"),
                    tag.getInt("pos1_z"));
            return finishCopy(level, stack, pos1, clicked, player)
                    ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        // --- Click 3+: paste (copy stays for repeated pastes) ---
        paste(level, stack, context, player);
        return InteractionResult.SUCCESS;
    }

    // ----------------------------- snapshot -------------------------------

    private boolean finishCopy(Level level, ItemStack stack, BlockPos a, BlockPos b, Player player) {
        int minX = Math.min(a.getX(), b.getX()), maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY()), maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ()), maxZ = Math.max(a.getZ(), b.getZ());
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        if (volume > MAX_COPY_VOLUME) {
            player.displayClientMessage(Component.literal(
                    "Region too large to copy! (" + volume + " blocks, max " + MAX_COPY_VOLUME + ")"), true);
            clearAll(stack);
            return false;
        }

        ListTag list = new ListTag();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;

                    CompoundTag e = new CompoundTag();
                    e.putInt("x", x - minX);
                    e.putInt("y", y - minY);
                    e.putInt("z", z - minZ);
                    e.put("state", NbtUtils.writeBlockState(s));
                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null) {
                        e.put("be", be.saveWithFullMetadata(level.registryAccess()));
                    }
                    list.add(e);
                }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove("pos1_x"); nbt.remove("pos1_y"); nbt.remove("pos1_z");
            nbt.put("blocks", list);
            nbt.putInt("copy_dir", dirIndex(player.getDirection()));
        });
        player.displayClientMessage(Component.literal(
                "Copied " + list.size() + " blocks. Click a surface to paste."), true);
        return true;
    }

    // ------------------------------- paste --------------------------------

    private void paste(Level level, ItemStack stack, UseOnContext context, Player player) {
        CompoundTag tag = getCustomTag(stack);
        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        if (blocks.isEmpty()) return;

        Rotation rot = rotationFor(tag.getInt("copy_dir"), dirIndex(player.getDirection()));
        BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        HolderGetter<Block> lookup = level.registryAccess().lookup(Registries.BLOCK).orElseThrow();

        List<BlockChange> changes = new ArrayList<>();
        int placed = 0;

        for (Tag t : blocks) {
            CompoundTag e = (CompoundTag) t;
            int dx = e.getInt("x");
            int dy = e.getInt("y");
            int dz = e.getInt("z");

            int[] off = rotateOffset(dx, dz, rot);
            BlockPos target = origin.offset(off[0], dy, off[1]);

            BlockState oldState = level.getBlockState(target);

            BlockState state = NbtUtils.readBlockState(lookup, e.getCompound("state"))
                    .rotate(rot);

            // Record the old state before overwriting
            if (!oldState.equals(state)) {
                changes.add(new BlockChange(target, oldState));
            }

            level.setBlock(target, state, 3);

            if (e.contains("be")) {
                CompoundTag beTag = e.getCompound("be");
                BlockEntity be = level.getBlockEntity(target);
                if (be != null && beTag != null) {
                    be.loadWithComponents(beTag, level.registryAccess());
                    be.setChanged();
                }
            }

            placed++;
        }

        // Save history for the Undo Trowel
        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("copy_paste", changes);
        }

        player.displayClientMessage(Component.literal(
                "Pasted " + placed + " blocks" + (rot == Rotation.NONE ? "." : " (" + rot.name() + ").")), true);
    }

    private void clearAll(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove("pos1_x"); nbt.remove("pos1_y"); nbt.remove("pos1_z");
            nbt.remove("blocks"); nbt.remove("copy_dir");
        });
    }
}