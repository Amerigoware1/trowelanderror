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

import com.trowelanderror.TrowelAndError;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TrowelAndError.MODID);

    public static final RegistryObject<Item> AIR_TROWEL = ITEMS.register("air_trowel",
            () -> new AirTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("air_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> GAP_FILL_TROWEL = ITEMS.register("gap_fill_trowel",
            () -> new GapFillTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("gap_fill_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> RANDOM_GAP_FILL_TROWEL = ITEMS.register("random_gap_fill_trowel",
            () -> new RandomGapFillTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("random_gap_fill_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> EXCHANGE_TROWEL = ITEMS.register("exchange_trowel",
            () -> new ExchangeTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("exchange_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> RANDOM_EXCHANGE_TROWEL = ITEMS.register("random_exchange_trowel",
            () -> new RandomExchangeTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("random_exchange_trowel"))
                    .stacksTo(1)));


    public static final RegistryObject<Item> FILL_DOWN_TROWEL = ITEMS.register("fill_down_trowel",
            () -> new FillDownTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("fill_down_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> RANDOM_FILL_DOWN_TROWEL = ITEMS.register("random_fill_down_trowel",
            () -> new RandomFillDownTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("random_fill_down_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> DIGGER_TROWEL = ITEMS.register("digger_trowel",
            () -> new DiggerTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("digger_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> COPY_PASTE_TROWEL = ITEMS.register("copy_paste_trowel",
            () -> new CopyPasteTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("copy_paste_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> FIRE_TROWEL = ITEMS.register("fire_trowel",
            () -> new FireTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("fire_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> UNDO_TROWEL = ITEMS.register("undo_trowel",
            () -> new UndoTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("undo_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> WATER_FILL_LADLE = ITEMS.register("water_fill_ladle",
            () -> new WaterFillLadleItem(new Item.Properties()
                    .setId(ITEMS.key("water_fill_ladle"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> LAVA_FILL_LADLE = ITEMS.register("lava_fill_ladle",
            () -> new LavaFillLadleItem(new Item.Properties()
                    .setId(ITEMS.key("lava_fill_ladle"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> DRAIN_LADLE = ITEMS.register("drain_ladle",
            () -> new DrainLadleItem(new Item.Properties()
                    .setId(ITEMS.key("drain_ladle"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> SPONGE_MOP = ITEMS.register("sponge_mop",
            () -> new SpongeMopItem(new Item.Properties()
                    .setId(ITEMS.key("sponge_mop"))
                    .stacksTo(1)));
}