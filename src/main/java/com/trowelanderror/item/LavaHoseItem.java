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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class LavaHoseItem extends Item {

    private static final int MAX_LENGTH = 32; // how far the hose can shoot

    public LavaHoseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Raytrace to find where the player is looking
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        BlockPos startPos;
        Direction face;

        if (hit.getType() == HitResult.Type.BLOCK) {
            startPos = hit.getBlockPos().relative(hit.getDirection());
            face = hit.getDirection();
        } else {
            // Looking at air – start from the block in front of the player’s eyes
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            startPos = BlockPos.containing(eye.add(look.scale(1.5)));
            face = Direction.getApproximateNearest(look);
        }

        int filled = sprayLava(level, startPos, face, player);

        if (filled > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.displayClientMessage(Component.literal("§bHose sprayed " + filled + " lava blocks."), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private int sprayLava(Level level, BlockPos start, Direction direction, Player player) {
        List<BlockChange> changes = new ArrayList<>();
        int filled = 0;

        // Sneaking = flowing lava, otherwise source lava
        BlockState lavaState = player.isShiftKeyDown()
                ? Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 1) // flowing
                : Blocks.LAVA.defaultBlockState(); // source

        BlockPos.MutableBlockPos cursor = start.mutable();

        for (int i = 0; i < MAX_LENGTH; i++) {
            BlockState current = level.getBlockState(cursor);

            // Stop if we hit a solid non-replaceable block
            if (!current.isAir() && !current.canBeReplaced(Fluids.LAVA)) {
                break;
            }

            if (!current.equals(lavaState)) {
                changes.add(new BlockChange(cursor.immutable(), current));
                level.setBlock(cursor, lavaState, 3);
                filled++;
            }

            cursor.move(direction);
        }

        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("lava_hose", changes);
        }

        return filled;
    }
}
