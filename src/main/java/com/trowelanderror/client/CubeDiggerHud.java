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

import com.trowelanderror.data.DiggerSettings;
import com.trowelanderror.item.DiggerTrowelItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "trowelanderror", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CubeDiggerHud {

    private static final int X = 20, Y = 20;

    // In 1.21.11 this event fires on the DEFAULT (Forge) bus group, NOT the mod bus!
    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                Identifier.fromNamespaceAndPath("trowelanderror", "cube_digger_hud"),
                CubeDiggerHud::renderHud);
    }

    public static void renderHud(GuiGraphics gfx, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof DiggerTrowelItem)) return;
        DiggerSettings s = DiggerTrowelItem.getSettings(held);
        gfx.drawString(mc.font, "Digger: " + s.shape() + " " + s.diameter() + "x" + s.diameter() + " d" + s.depth() + (s.spareOres() ? " [ores]" : ""), 4, 4, 0xFFFFFFFF);
    }

}