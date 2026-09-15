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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class SpongeMopItem extends Item {

    private static final int RADIUS = 6; // 13×13×13 area

    public SpongeMopItem(Properties properties) {
        super(properties);
    }

    // Called when right-clicking air or fluid
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);

        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);

        if (hit.getType() == HitResult.Type.BLOCK) {
            int drained = mopArea(level, hit.getBlockPos(), player);
            if (drained > 0) {
                level.playSound(null, hit.getBlockPos(), SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("§eMopped up " + drained + " fluid blocks."), true);
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    // Called when right-clicking a solid block
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        int drained = mopArea(level, context.getClickedPos(), player);

        if (drained > 0) {
            level.playSound(null, context.getClickedPos(), SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("§eMopped up " + drained + " fluid blocks."), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private int mopArea(Level level, BlockPos center, Player player) {
        List<BlockChange> changes = new ArrayList<>();
        int drained = 0;

        int minX = center.getX() - RADIUS;
        int maxX = center.getX() + RADIUS;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - RADIUS);
        int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + RADIUS);
        int minZ = center.getZ() - RADIUS;
        int maxZ = center.getZ() + RADIUS;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState old = level.getBlockState(pos);
                    FluidState fluid = old.getFluidState();

                    if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER) ||
                            fluid.is(Fluids.LAVA)  || fluid.is(Fluids.FLOWING_LAVA)) {

                        changes.add(new BlockChange(pos, old));
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        drained++;
                    }
                }
            }
        }

        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("sponge_mop", changes);
        }

        return drained;
    }
}