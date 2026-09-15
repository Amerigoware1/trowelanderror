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
import com.trowelanderror.util.VariantConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomGapFillTrowelItem extends BaseTrowelItem {

    private static final int MAX_VOLUME = 32768;
    private static final Random RANDOM = new Random();

    public RandomGapFillTrowelItem(Properties properties) {
        super(properties);
    }

    // Right‑click on AIR → clear selection (same as before)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

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
        return InteractionResultHolder.success(stack);
    }

    // Right‑click on BLOCK
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

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        // --- First click: store Point A and the block type ---
        if (!tag.contains("pos1_x")) {
            BlockState clickedState = level.getBlockState(clickedPos);
            String blockId = BuiltInRegistries.BLOCK.getKey(clickedState.getBlock()).toString();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
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

        // --- Second click: fill with random variants ---
        BlockPos pos1 = new BlockPos(
                tag.getInt("pos1_x"),
                tag.getInt("pos1_y"),
                tag.getInt("pos1_z")
        );
        BlockPos pos2 = clickedPos;

        String blockId = tag.getString("block_id");
        if (blockId.isEmpty()) {
            player.displayClientMessage(Component.literal("No fill block stored – please set Point A again."), true);
            clearSelection(stack);
            return InteractionResult.FAIL;
        }

        Block fillBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));
        BlockState fillState = fillBlock.defaultBlockState();

        // Perform random fill
        fillBoxWithVariants(level, pos1, pos2, fillState, player);

        clearSelection(stack);
        return InteractionResult.SUCCESS;
    }

    private void clearSelection(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove("pos1_x");
            nbt.remove("pos1_y");
            nbt.remove("pos1_z");
            nbt.remove("block_id");
        });
    }

    // --- Random fill logic ---
    private void fillBoxWithVariants(Level level, BlockPos pos1, BlockPos pos2,
                                     BlockState baseState, Player player) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > MAX_VOLUME) {
            player.displayClientMessage(
                    Component.literal("Selection too large! (" + volume + " blocks, max is " + MAX_VOLUME + ")"),
                    true
            );
            return;
        }

        // Get list of possible variants for the base block
        List<BlockState> variants = VariantConfig.getVariantStates(baseState.getBlock());

        List<BlockChange> changes = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState oldState = level.getBlockState(pos);

                    BlockState stateToPlace;
                    if (!variants.isEmpty() && RANDOM.nextDouble() < 0.2) { // 20% chance for a variant
                        stateToPlace = variants.get(RANDOM.nextInt(variants.size()));
                    } else {
                        stateToPlace = baseState;
                    }

                    // Only record the change if the block is actually different
                    if (!oldState.equals(stateToPlace)) {
                        changes.add(new BlockChange(pos, oldState));
                        level.setBlock(pos, stateToPlace, 3);
                    }
                }
            }
        }

        // Record the action so Undo Trowel can reverse it
        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("random_fill", changes);   // or "fill" if you prefer the same name
        }

        player.displayClientMessage(
                Component.literal("Filled " + changes.size() + " blocks (with variants)."),
                true
        );
    }
}