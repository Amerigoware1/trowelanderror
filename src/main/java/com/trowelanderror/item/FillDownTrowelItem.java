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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

public class FillDownTrowelItem extends Item {

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
        BlockState baseState = level.getBlockState(clickedPos);
        if (baseState.isAir()) return InteractionResult.PASS;

        int placed = 0;

        if (face == Direction.UP && player != null) {
            // Top face: forward strip (3 wide) + side columns
            Direction facing = player.getDirection();
            BlockPos forwardCenter = clickedPos.relative(facing);
            Direction left = facing.getCounterClockWise();
            Direction right = facing.getClockWise();

            List<BlockPos> starts = new ArrayList<>();
            starts.add(forwardCenter);
            starts.add(forwardCenter.relative(left));
            starts.add(forwardCenter.relative(right));

            BlockPos sideLeft = clickedPos.relative(left);
            BlockPos sideRight = clickedPos.relative(right);
            if (isReplaceable(level, sideLeft)) starts.add(sideLeft);
            if (isReplaceable(level, sideRight)) starts.add(sideRight);

            placed += fillDownStrip(level, starts.toArray(new BlockPos[0]), baseState);
        } else {
            // Side face: single column
            BlockPos startPos = clickedPos.relative(face);
            placed += fillDownStrip(level, new BlockPos[]{startPos}, baseState);
        }

        if (placed > 0) {
            level.playSound(null, clickedPos,
                    baseState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return placed > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private int fillDownStrip(Level level, BlockPos[] starts, BlockState fillState) {
        int placed = 0;
        List<BlockChange> changes = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (BlockPos startPos : starts) {
            for (int y = startPos.getY(); y >= level.getMinY() && placed < MAX_FILL_DEPTH; y--) {
                cursor.set(startPos.getX(), y, startPos.getZ());

                if (!isReplaceable(level, cursor)) {
                    break; // stop this column
                }

                BlockState oldState = level.getBlockState(cursor);

                // Only record + place if the block is actually different
                if (!oldState.equals(fillState)) {
                    changes.add(new BlockChange(cursor.immutable(), oldState));
                    level.setBlockAndUpdate(cursor, fillState);
                    placed++;
                }
            }
        }

        // Record history for the Undo Trowel
        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("fill_down", changes);
        }

        return placed;
    }

    private boolean isReplaceable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        FluidState fluid = state.getFluidState();
        return fluid.is(FluidTags.WATER) || fluid.is(FluidTags.LAVA);
    }
}