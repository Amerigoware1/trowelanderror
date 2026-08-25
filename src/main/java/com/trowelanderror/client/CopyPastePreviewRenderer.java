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

package com.trowelanderror.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trowelanderror.item.CopyPasteTrowelItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "trowelanderror",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CopyPastePreviewRenderer {

    private static final int MAX_PREVIEW_BOXES = 2048;
    private static final VoxelShape UNIT_CUBE = Shapes.create(new AABB(0, 0, 0, 1, 1, 1));

    private CopyPastePreviewRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof CopyPasteTrowelItem)) return;

        CompoundTag itemTag = CopyPasteTrowelItem.getCustomTag(stack);
        if (!itemTag.contains("blocks")) return;

        ListTag blocks = itemTag.getList("blocks").orElseGet(ListTag::new);
        if (blocks.isEmpty()) return;

        BlockHitResult hit = event.getTarget();
        Rotation rotation = CopyPasteTrowelItem.rotationFor(
                itemTag.getInt("copy_dir").orElse(0),
                CopyPasteTrowelItem.dirIndex(player.getDirection()));
        BlockPos origin = hit.getBlockPos().relative(hit.getDirection());
        Vec3 cameraPos = event.getCamera().position(); // renamed in 1.21.11

        HolderGetter<Block> blockLookup = minecraft.level.registryAccess()
                .lookup(Registries.BLOCK)
                .orElseThrow();

        event.setCustomRenderer((bufferSource, poseStack, translucentPass, state) -> {
            if (translucentPass) return; // runs once per pass; draw only once

            VertexConsumer lineBuffer = bufferSource.getBuffer(RenderTypes.lines());

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            int drawn = 0;
            for (Tag rawEntry : blocks) {
                if (drawn >= MAX_PREVIEW_BOXES) break;
                if (!(rawEntry instanceof CompoundTag entry)) continue;

                int dx = entry.getInt("x").orElse(0);
                int dy = entry.getInt("y").orElse(0);
                int dz = entry.getInt("z").orElse(0);

                int[] rotatedOffset = CopyPasteTrowelItem.rotateOffset(dx, dz, rotation);
                BlockPos previewPos = origin.offset(rotatedOffset[0], dy, rotatedOffset[1]);

                CompoundTag stateTag = entry.getCompound("state").orElse(null);
                if (stateTag == null) continue;

                BlockState copiedState = NbtUtils.readBlockState(blockLookup, stateTag);
                int rgb = copiedState.getMapColor(minecraft.level, previewPos).col;
                int argb = 0xBF000000 | (rgb & 0xFFFFFF); // ~75% alpha + map color

                ShapeRenderer.renderShape(poseStack, lineBuffer, UNIT_CUBE,
                        previewPos.getX(), previewPos.getY(), previewPos.getZ(),
                        argb, 2.0F);
                drawn++;
            }

            poseStack.popPose();
            bufferSource.endBatch(RenderTypes.lines());
        });
    }
}