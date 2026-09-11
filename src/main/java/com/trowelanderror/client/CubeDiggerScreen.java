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

import com.trowelanderror.TrowelAndError;
import com.trowelanderror.data.DiggerSettings;
import com.trowelanderror.data.DiggerSettings.Shape;
import com.trowelanderror.item.DiggerTrowelItem;
import com.trowelanderror.network.SetDiggerSettingsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

public class CubeDiggerScreen extends Screen {

    private static final int PANEL_W = 120;
    private static final int PANEL_H = 92;

    private int depth;
    private boolean spareOres;

    protected CubeDiggerScreen() {
        super(Component.literal("Cube Digger Modes"));
    }

    @Override
    protected void init() {
        super.init();
        DiggerSettings cur = currentSettings();
        depth = Math.min(6, Math.max(1, cur.depth()));
        spareOres = cur.spareOres();

        int x = this.width / 2 - PANEL_W / 2;
        int y = this.height / 2 - PANEL_H / 2;

        this.addRenderableWidget(Button.builder(Component.literal("[Square 3x3]"), b -> applyAndClose(Shape.SQUARE))
                .bounds(x, y + 18, PANEL_W, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("[Circle 5x5]"), b -> applyAndClose(Shape.CIRCLE))
                .bounds(x, y + 34, PANEL_W, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("[Diamond 7x7]"), b -> applyAndClose(Shape.DIAMOND))
                .bounds(x, y + 50, PANEL_W, 15).build());

        this.addRenderableWidget(Button.builder(Component.literal("Depth: " + depth), this::cycleDepth)
                .bounds(x, y + 70, 58, 16).build());
        this.addRenderableWidget(Button.builder(spareOresLabel(), this::toggleOres)
                .bounds(x + 62, y + 70, 58, 16).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0x88000000);
        int x = this.width / 2 - PANEL_W / 2;
        int y = this.height / 2 - PANEL_H / 2;
        gfx.fill(x, y, x + PANEL_W, y + PANEL_H, 0xCC000000);
        gfx.drawString(this.font, "Cube Digger Modes:", x + 5, y + 5, 0xFFFFFFFF);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private Component spareOresLabel() {
        return Component.literal(spareOres ? "[X] Ores" : "[ ] Ores");
    }

    private void cycleDepth(Button button) {
        depth = depth >= 6 ? 1 : depth + 1;
        button.setMessage(Component.literal("Depth: " + depth));
        save(null);
    }

    private void toggleOres(Button button) {
        spareOres = !spareOres;
        button.setMessage(spareOresLabel());
        save(null);
    }

    private void applyAndClose(Shape shape) {
        save(shape);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int d = diameterFor(shape);
            player.displayClientMessage(Component.literal(
                    "Cube Digger: " + shape + " " + d + "x" + d + ", depth " + depth
                            + (spareOres ? ", ores spared" : "")), true);
        }
        this.onClose();
    }

    private void save(Shape newShape) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof DiggerTrowelItem)) return;

        Shape shape = newShape != null ? newShape : DiggerTrowelItem.getSettings(held).shape();
        DiggerSettings newSettings = new DiggerSettings(shape, diameterFor(shape), depth, spareOres);

        // Update client immediately so HUD feels responsive
        DiggerTrowelItem.setSettings(held, newSettings);

        // Send to server
        TrowelAndError.CHANNEL.send(
                new SetDiggerSettingsPacket(newSettings),
                PacketDistributor.SERVER.noArg()
        );
    }

    private static int diameterFor(Shape shape) {
        return switch (shape) {
            case CIRCLE -> 5;
            case DIAMOND -> 7;
            default -> 3;
        };
    }

    private DiggerSettings currentSettings() {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof DiggerTrowelItem) {
            return DiggerTrowelItem.getSettings(player.getMainHandItem());
        }
        return DiggerSettings.DEFAULT;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}