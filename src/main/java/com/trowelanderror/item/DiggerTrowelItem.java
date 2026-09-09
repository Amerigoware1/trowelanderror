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

import com.trowelanderror.data.DiggerSettings;
import com.trowelanderror.history.BlockChange;
import com.trowelanderror.history.HistoryManager;
import com.trowelanderror.setup.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class DiggerTrowelItem extends Item {

    // #minecraft:ores (vanilla) and #c:ores (the convention tag modded ores use)
    private static final TagKey<Block> VANILLA_ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "ores"));
    private static final TagKey<Block> COMMON_ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

    public DiggerTrowelItem(Properties props) {
        super(props);
    }

    public static DiggerSettings getSettings(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DIGGER_SETTINGS.get(), DiggerSettings.DEFAULT);
    }

    public static void setSettings(ItemStack stack, DiggerSettings settings) {
        stack.set(ModDataComponents.DIGGER_SETTINGS.get(), settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().mayBuild) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        DiggerSettings settings = getSettings(stack);

        List<BlockPos> mask = buildMask(clicked, context.getClickedFace(), settings);

        List<BlockChange> changes = new ArrayList<>();
        int broken = 0;

        for (BlockPos pos : mask) {
            BlockState st = level.getBlockState(pos);
            if (st.isAir()) continue;
            if (settings.spareOres() && (st.is(VANILLA_ORES) || st.is(COMMON_ORES))) continue;

            // Record the block before destroying it
            changes.add(new BlockChange(pos, st));
            level.destroyBlock(pos, true, player);
            broken++;
        }

        // Save history for the Undo Trowel
        if (!changes.isEmpty() && level.getServer() != null) {
            HistoryManager.getInstance(level.getServer())
                    .recordAction("digger", changes);
        }

        if (broken > 0) {
            level.playSound(null, clicked,
                    clickedState.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return broken > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static BlockPos offsetAlong(BlockPos p, Direction.Axis axis, int amount) {
        return switch (axis) {
            case X -> p.offset(amount, 0, 0);
            case Y -> p.offset(0, amount, 0);
            case Z -> p.offset(0, 0, amount);
        };
    }

    private List<BlockPos> buildMask(BlockPos clicked, Direction face, DiggerSettings s) {
        int r = s.diameter() / 2;
        Direction.Axis n = face.getAxis();
        // The two axes perpendicular to the clicked face = the shape's plane
        Direction.Axis axisU = (n == Direction.Axis.X) ? Direction.Axis.Y : Direction.Axis.X;
        Direction.Axis axisV = (n == Direction.Axis.Z) ? Direction.Axis.Y : Direction.Axis.Z;
        Direction digDir = face.getOpposite(); // depth marches INTO the block

        List<BlockPos> list = new ArrayList<>();
        for (int u = -r; u <= r; u++) {
            for (int v = -r; v <= r; v++) {
                boolean in = switch (s.shape()) {
                    case CIRCLE -> u * u + v * v <= r * r;
                    case DIAMOND -> Math.abs(u) + Math.abs(v) <= r;
                    default -> true;
                };
                if (!in) continue;
                BlockPos column = offsetAlong(offsetAlong(clicked, axisU, u), axisV, v);
                for (int t = 0; t < s.depth(); t++) {
                    list.add(column.relative(digDir, t));
                }
            }
        }
        return list;
    }
}