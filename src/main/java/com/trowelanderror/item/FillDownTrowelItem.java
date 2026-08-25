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
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A trowel that "smears" the clicked block out by one block, and then fills straight down.
 *
 * Example: Click the side of a castle wall to instantly generate a 1x1 support column
 * extending down to the ground. Click the top face of a cliff edge to extend the ground outward.
 */
public class FillDownTrowelItem extends Item {

    /** Safety valve so a click over a huge ravine can't place thousands of blocks at once. */
    private static final int MAX_FILL_DEPTH = 512;

    public FillDownTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().mayBuild) return InteractionResult.PASS;

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState fillState = level.getBlockState(clickedPos);
        if (fillState.isAir()) return InteractionResult.PASS;

        int placed = 0;

        if (face == Direction.UP && player != null) {
            // --- Top face: smear outward 3 wide by 1 deep ---
            Direction facing = player.getDirection();
            BlockPos start = clickedPos.relative(facing);

            // Center + left + right relative to facing
            Direction left = facing.getCounterClockWise();
            Direction right = facing.getClockWise();

            BlockPos[] strip = {
                    start,
                    start.relative(left),
                    start.relative(right)
            };

            placed += fillDownStrip(level, strip, fillState);
        } else {
            // --- Side face: original 1x1 column ---
            BlockPos startPos = clickedPos.relative(face);
            placed += fillDownStrip(level, new BlockPos[]{startPos}, fillState);
        }

        if (placed > 0) {
            level.playSound(null, clickedPos,
                    fillState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return placed > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    private int fillDownStrip(Level level, BlockPos[] starts, BlockState fillState) {
        int placed = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (BlockPos startPos : starts) {
            for (int y = startPos.getY(); y >= level.getMinY() && placed < MAX_FILL_DEPTH; y--) {
                cursor.set(startPos.getX(), y, startPos.getZ());
                if (!level.getBlockState(cursor).isAir()) break;

                level.setBlockAndUpdate(cursor, fillState);
                placed++;
            }
        }
        return placed;
    }
}