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

import com.trowelanderror.util.VariantConfig;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class RandomUniversalFillTrowelItem extends BaseTrowelItem {

    private static final int MAX_VOLUME = 32768;
    private static final Random RANDOM = new Random();

    public RandomUniversalFillTrowelItem(Properties properties) {
        super(properties);
    }

    // Right‑click on AIR → clear selection (same as before)
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        clearSelection(stack);
        player.displayClientMessage(Component.literal("Cleared fill selection."), true);
        return InteractionResult.SUCCESS;
    }

    // Right‑click on BLOCK
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

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
                tag.getInt("pos1_x").orElse(0),
                tag.getInt("pos1_y").orElse(0),
                tag.getInt("pos1_z").orElse(0)
        );
        BlockPos pos2 = clickedPos;

        String blockId = tag.getString("block_id").orElse("");
        if (blockId.isEmpty()) {
            player.displayClientMessage(Component.literal("No fill block stored – please set Point A again."), true);
            clearSelection(stack);
            return InteractionResult.FAIL;
        }

        Optional<Holder.Reference<Block>> fillBlock = BuiltInRegistries.BLOCK.get(Identifier.parse(blockId));
        BlockState baseState = fillBlock
                .map(ref -> ref.value().defaultBlockState())
                .orElse(Blocks.AIR.defaultBlockState());

        // Perform random fill
        fillBoxWithVariants(level, pos1, pos2, baseState, player);

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

        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > MAX_VOLUME) {
            player.displayClientMessage(
                    Component.literal("Selection too large! (" + volume + " blocks, max is " + MAX_VOLUME + ")"),
                    true
            );
            return;
        }

        // Get list of possible variants for the base block
        List<BlockState> variants = VariantConfig.getVariantStates(baseState.getBlock());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState stateToPlace;
                    if (!variants.isEmpty() && RANDOM.nextDouble() < 0.2) { // or your own chance
                        stateToPlace = variants.get(RANDOM.nextInt(variants.size()));
                    } else {
                        stateToPlace = baseState;
                    }
                    level.setBlock(new BlockPos(x, y, z), stateToPlace, 3);
                }
            }
        }

        player.displayClientMessage(
                Component.literal("Filled " + volume + " blocks (with random variants)!"),
                true
        );
    }
}
    // --- Helper: define related blocks for each "family" ---
    /*private List<BlockState> getVariantsFor(Block block) {
        // Hardcoded mapping – extend as needed.
        // Alternatively, use tags (e.g., #stone_bricks) to dynamically fetch variants.
        if (block == Blocks.STONE_BRICKS) {
            return List.of(
                    Blocks.STONE_BRICKS.defaultBlockState(),
                    Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                    Blocks.STONE.defaultBlockState()   // occasional plain stone
            );
        }
        if (block == Blocks.BRICKS) {
            return List.of(
                    Blocks.BRICKS.defaultBlockState(),
                    Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), // placeholder
                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
            );
        }
        if (block == Blocks.OAK_PLANKS) {
            return List.of(
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    Blocks.SPRUCE_PLANKS.defaultBlockState(),
                    Blocks.BIRCH_PLANKS.defaultBlockState()
            );
        }
        // Add more families as desired…
        return List.of(); // no variants
    }
}*/