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

import com.azure.json.implementation.jackson.core.TreeNode;
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

    public static final RegistryObject<Item> UNIVERSAL_FILL_TROWEL = ITEMS.register("universal_fill_trowel",
            () -> new UniversalFillTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("universal_fill_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> EXCHANGE_TROWEL = ITEMS.register("exchange_trowel",
            () -> new ExchangeTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("exchange_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> COPY_PASTE_TROWEL = ITEMS.register("copy_paste_trowel",
            () -> new CopyPasteTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("copy_paste_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> FILL_DOWN_TROWEL = ITEMS.register("fill_down_trowel",
            () -> new FillDownTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("fill_down_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> DIGGER_TROWEL = ITEMS.register("digger_trowel",
            () -> new DiggerTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("digger_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> RANDOM_UNIVERSAL_FILL_TROWEL = ITEMS.register("ranom_universal_fill_trowel",
            () -> new RandomUniversalFillTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("random_universal_fill_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> RANDOM_FILL_DOWN_TROWEL = ITEMS.register("random_fill_down_trowel",
            () -> new RandomFillDownTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("random_fill_down_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> RANDOM_EXCHANGE_TROWEL = ITEMS.register("random_exchange_trowel",
            () -> new RandomExchangeTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("random_exchange_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> FIRE_TROWEL = ITEMS.register("fire_trowel",
            () -> new FireTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("fire_trowel"))
                    .stacksTo(1)));

    public static final RegistryObject<Item> UNDO_TROWEL = ITEMS.register("undo_trowel",
            () -> new UndoTrowelItem(new Item.Properties()
                    .setId(ITEMS.key("undo_trowel"))
                    .stacksTo(1)));
}