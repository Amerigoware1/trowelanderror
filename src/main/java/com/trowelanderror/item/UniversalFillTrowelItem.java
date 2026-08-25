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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.context.UseOnContext;   // fixed
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState; // fixed
import net.minecraft.world.level.block.Blocks;
import java.util.Optional;

public class UniversalFillTrowelItem extends BaseTrowelItem {

    private static final int MAX_VOLUME = 32768; // Safety limit (32x32x32)

    public UniversalFillTrowelItem(Properties properties) {
        super(properties);
    }

    // Right‑click on AIR → clear everything
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove("pos1_x");
            nbt.remove("pos1_y");
            nbt.remove("pos1_z");
            nbt.remove("block_id");
        });

        player.displayClientMessage(
                Component.literal("Cleared fill selection."),
                true
        );
        return InteractionResult.SUCCESS;
    }

    // Right‑click on a BLOCK
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack trowelStack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        CustomData customData = trowelStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();

        // --- If Point A is NOT set yet, this is the first click ---
        if (!tag.contains("pos1_x")) {
            // Remember the clicked block as the fill material
            BlockState clickedState = level.getBlockState(clickedPos);
            String blockId = BuiltInRegistries.BLOCK.getKey(clickedState.getBlock()).toString();

            CustomData.update(DataComponents.CUSTOM_DATA, trowelStack, nbt -> {
                nbt.putInt("pos1_x", clickedPos.getX());
                nbt.putInt("pos1_y", clickedPos.getY());
                nbt.putInt("pos1_z", clickedPos.getZ());
                nbt.putString("block_id", blockId);
            });

            player.displayClientMessage(
                    Component.literal("Point A set at " + clickedPos.toShortString() +
                            " with " + clickedState.getBlock().getName().getString()),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        // --- Point A exists → this is the second click (Point B) ---
        BlockPos pos1 = new BlockPos(
                tag.getInt("pos1_x").orElse(0),
                tag.getInt("pos1_y").orElse(0),
                tag.getInt("pos1_z").orElse(0)
        );
        BlockPos pos2 = clickedPos;

// Read the stored block ID (getString returns Optional<String>)
        String blockId = tag.getString("block_id").orElse("");
        if (blockId.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("No fill block stored – please set Point A again."),
                    true
            );
            clearSelection(trowelStack);
            return InteractionResult.FAIL;
        }

// 1. Lookup returns Optional<Holder.Reference<Block>>
        Optional<Holder.Reference<Block>> fillBlock = BuiltInRegistries.BLOCK.get(Identifier.parse(blockId));

// 2. Unwrap the Optional, get the value from the Reference, and get its state
        BlockState fillState = fillBlock
                .map(ref -> ref.value().defaultBlockState())
                .orElse(Blocks.AIR.defaultBlockState()); // Fallback if the block ID is invalid

// Perform the fill
        fillBox(level, pos1, pos2, fillState, player);

// Clear stored data for the next selection
        clearSelection(trowelStack);
        return InteractionResult.SUCCESS;
    }

    // Helper to remove all our stored NBT
    private void clearSelection(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove("pos1_x");
            nbt.remove("pos1_y");
            nbt.remove("pos1_z");
            nbt.remove("block_id");
        });
    }

    // The fill logic (unchanged)
    private void fillBox(Level level, BlockPos pos1, BlockPos pos2, BlockState state, Player player) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        if (volume > MAX_VOLUME) {
            player.displayClientMessage(
                    Component.literal("Selection too large! (" + volume + " blocks, max is " + MAX_VOLUME + ")"),
                    true
            );
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    level.setBlock(new BlockPos(x, y, z), state, 3);
                }
            }
        }

        player.displayClientMessage(
                Component.literal("Filled " + volume + " blocks!"),
                true
        );
    }
}