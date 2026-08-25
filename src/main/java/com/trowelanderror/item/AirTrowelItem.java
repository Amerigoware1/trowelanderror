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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerPlayer;

public class AirTrowelItem extends BaseTrowelItem {

    public AirTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult hitResult = rayTraceWithFluids(level, player);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos clickedPos = hitResult.getBlockPos();

        // Only modify the world on the server.
        if (!level.isClientSide()) {
            CompoundTag tag = getTrowelData(stack);

            if (!tag.contains("PointA_X")) {
                tag.putInt("PointA_X", clickedPos.getX());
                tag.putInt("PointA_Y", clickedPos.getY());
                tag.putInt("PointA_Z", clickedPos.getZ());

                saveTrowelData(stack, tag);

                ((ServerPlayer) player).sendSystemMessage(
                        Component.literal(
                                "§aPoint A set to: " + clickedPos.toShortString()
                        )
                );

            } else {
                BlockPos posA = new BlockPos(
                        tag.getInt("PointA_X").orElse(0),
                        tag.getInt("PointA_Y").orElse(0),
                        tag.getInt("PointA_Z").orElse(0)
                );

                BlockPos posB = clickedPos;

                int clearedCount = clearRegion(level, posA, posB);

                ((ServerPlayer) player).sendSystemMessage(
                        Component.literal(
                                "§eCleared " + clearedCount
                                        + " blocks!"
                        )
                );

                tag.remove("PointA_X");
                tag.remove("PointA_Y");
                tag.remove("PointA_Z");

                saveTrowelData(stack, tag);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private int clearRegion(Level level, BlockPos posA, BlockPos posB) {
        int minX = Math.min(posA.getX(), posB.getX());
        int maxX = Math.max(posA.getX(), posB.getX());

        int minY = Math.min(posA.getY(), posB.getY());
        int maxY = Math.max(posA.getY(), posB.getY());

        int minZ = Math.min(posA.getZ(), posB.getZ());
        int maxZ = Math.max(posA.getZ(), posB.getZ());

        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    BlockPos target = new BlockPos(x, y, z);

                    if (!level.isEmptyBlock(target)) {
                        level.setBlock(
                                target,
                                Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL
                        );

                        count++;
                    }
                }
            }
        }

        return count;
    }
}