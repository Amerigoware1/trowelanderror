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

package com.trowelanderror.network;

import com.trowelanderror.data.DiggerSettings;
import com.trowelanderror.item.DiggerTrowelItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record SetDiggerSettingsPacket(DiggerSettings settings) implements CustomPacketPayload {

    public static final Type<SetDiggerSettingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("trowelanderror", "set_digger_settings"));

    public static final StreamCodec<FriendlyByteBuf, SetDiggerSettingsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    // Simple manual encoding for the record
                    new StreamCodec<FriendlyByteBuf, DiggerSettings>() {
                        @Override
                        public DiggerSettings decode(FriendlyByteBuf buf) {
                            DiggerSettings.Shape shape = buf.readEnum(DiggerSettings.Shape.class);
                            int diameter = buf.readVarInt();
                            int depth = buf.readVarInt();
                            boolean spareOres = buf.readBoolean();
                            return new DiggerSettings(shape, diameter, depth, spareOres);
                        }

                        @Override
                        public void encode(FriendlyByteBuf buf, DiggerSettings settings) {
                            buf.writeEnum(settings.shape());
                            buf.writeVarInt(settings.diameter());
                            buf.writeVarInt(settings.depth());
                            buf.writeBoolean(settings.spareOres());
                        }
                    },
                    SetDiggerSettingsPacket::settings,
                    SetDiggerSettingsPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Called on the server
    public static void handle(SetDiggerSettingsPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof DiggerTrowelItem) {
                DiggerTrowelItem.setSettings(stack, packet.settings());
            }
        });
        ctx.setPacketHandled(true);
    }
}