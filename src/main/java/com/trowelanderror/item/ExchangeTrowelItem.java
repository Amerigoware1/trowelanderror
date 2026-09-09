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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class ExchangeTrowelItem extends BaseTrowelItem {

    private static final int MAX_VOLUME = 32768;

    public ExchangeTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            clearSelection(stack);
            player.displayClientMessage(Component.literal("Exchange selection cleared."), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        String sourceId = tag.getString("source_block").orElse("");
        String targetId = tag.getString("target_block").orElse("");
        boolean hasPos2 = tag.contains("pos2_x");

        // --- STATE 0: No source → set source and Point A ---
        if (sourceId.isEmpty()) {
            String newSourceId = BuiltInRegistries.BLOCK.getKey(clickedState.getBlock()).toString();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
                nbt.putString("source_block", newSourceId);
                nbt.putInt("pos1_x", clickedPos.getX());
                nbt.putInt("pos1_y", clickedPos.getY());
                nbt.putInt("pos1_z", clickedPos.getZ());
            });
            player.displayClientMessage(
                    Component.literal("Source set to: " + clickedState.getBlock().getName().getString() +
                            " at Point A. Click a block to set target material."), true);
            return InteractionResult.SUCCESS;
        }

        // --- STATE 1: Source set, no target → set target ---
        if (targetId.isEmpty()) {
            String newTargetId = BuiltInRegistries.BLOCK.getKey(clickedState.getBlock()).toString();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
                nbt.putString("target_block", newTargetId);
            });
            player.displayClientMessage(
                    Component.literal("Target set to: " + clickedState.getBlock().getName().getString() +
                            ". Click Point B to define the exchange region."), true);
            return InteractionResult.SUCCESS;
        }

        // --- STATE 2: Both set, but no Point B yet → set Point B and execute ---
        if (!hasPos2) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
                nbt.putInt("pos2_x", clickedPos.getX());
                nbt.putInt("pos2_y", clickedPos.getY());
                nbt.putInt("pos2_z", clickedPos.getZ());
            });

            BlockPos pos1 = new BlockPos(
                    tag.getInt("pos1_x").orElse(0),
                    tag.getInt("pos1_y").orElse(0),
                    tag.getInt("pos1_z").orElse(0)
            );
            BlockPos pos2 = clickedPos;

            Block sourceBlock = BuiltInRegistries.BLOCK.get(Identifier.parse(sourceId))
                    .map(ref -> ref.value()).orElse(Blocks.AIR);
            Block targetBlock = BuiltInRegistries.BLOCK.get(Identifier.parse(targetId))
                    .map(ref -> ref.value()).orElse(Blocks.AIR);

            if (sourceBlock == Blocks.AIR || targetBlock == Blocks.AIR) {
                player.displayClientMessage(Component.literal("Error: Invalid blocks stored. Clearing."), true);
                clearSelection(stack);
                return InteractionResult.FAIL;
            }

            exchangeBox(level, pos1, pos2, sourceBlock, targetBlock.defaultBlockState(), player);

            clearSelection(stack);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void clearSelection(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove("source_block");
            nbt.remove("target_block");
            nbt.remove("pos1_x");
            nbt.remove("pos1_y");
            nbt.remove("pos1_z");
            nbt.remove("pos2_x");
            nbt.remove("pos2_y");
            nbt.remove("pos2_z");
        });
    }

    private void exchangeBox(Level level, BlockPos pos1, BlockPos pos2,
                             Block sourceBlock, BlockState targetState, Player player) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > MAX_VOLUME) {
            player.displayClientMessage(
                    Component.literal("Selection too large! (" + volume + " blocks, max is " + MAX_VOLUME + ")"), true);
            return;
        }

        List<BlockChange> changes = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState old = level.getBlockState(pos);

                    // Only exchange the source block
                    if (old.getBlock() == sourceBlock) {
                        if (!old.equals(targetState)) {
                            changes.add(new BlockChange(pos, old));
                            level.setBlock(pos, targetState, 3);
                        }
                    }
                }
            }
        }

        // Record for Undo Trowel
        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("exchange", changes);
        }

        player.displayClientMessage(
                Component.literal("Exchanged " + changes.size() + " blocks."), true);
    }
}