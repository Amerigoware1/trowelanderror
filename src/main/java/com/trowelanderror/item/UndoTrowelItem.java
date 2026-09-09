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

import com.trowelanderror.history.ActionEntry;
import com.trowelanderror.history.BlockChange;
import com.trowelanderror.history.HistoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class UndoTrowelItem extends Item {
    public UndoTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return performUndo(level, player);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        return performUndo(context.getLevel(), context.getPlayer());
    }

    private InteractionResult performUndo(Level level, Player player) {
        if (level.getServer() == null) return InteractionResult.FAIL;
        HistoryManager mgr = HistoryManager.getInstance(level.getServer());
        ActionEntry entry = mgr.getLastAction();
        if (entry == null) {
            player.displayClientMessage(Component.literal("Nothing to undo."), true);
            return InteractionResult.FAIL;
        }

        // Revert changes
        int reverted = 0;
        for (BlockChange change : entry.getChanges()) {
            BlockPos pos = change.getPos();
            // Only undo if the block is still the same as when we recorded? Actually, we just set it back.
            // Optionally check if the current block matches the new state? We can skip check for simplicity.
            level.setBlock(pos, change.getOldState(), 3);
            reverted++;
        }

        // Remove the action from history
        mgr.removeLastAction();

        player.displayClientMessage(Component.literal("Undid " + entry.getTrowelType() + " action (" + reverted + " blocks restored)."), true);
        return InteractionResult.SUCCESS;
    }
}
