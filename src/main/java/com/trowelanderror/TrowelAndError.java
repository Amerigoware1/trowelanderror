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

package com.trowelanderror;

import com.mojang.logging.LogUtils;
import com.trowelanderror.item.ModItems;
import com.trowelanderror.setup.ModDataComponents; // <--- ADD THIS IMPORT
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(TrowelAndError.MODID)
public final class TrowelAndError {
    public static final String MODID = "trowelanderror";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Deferred Registers
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creative Tab Registration
    public static final RegistryObject<CreativeModeTab> TROWEL_TAB = CREATIVE_MODE_TABS.register("trowel_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Trowel & Error"))
                    .icon(() -> new ItemStack(ModItems.AIR_TROWEL.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.AIR_TROWEL.get());
                        output.accept(ModItems.UNIVERSAL_FILL_TROWEL.get());
                        output.accept(ModItems.EXCHANGE_TROWEL.get());
                        output.accept(ModItems.COPY_PASTE_TROWEL.get());
                        output.accept(ModItems.FILL_DOWN_TROWEL.get());
                        output.accept(ModItems.DIGGER_TROWEL.get());
                    })
                    .build());

    public TrowelAndError(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register items, blocks, and tabs to the mod event bus
        ModItems.ITEMS.register(modBusGroup);
        BLOCKS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);

        // <--- ADD THIS LINE TO REGISTER YOUR DATA COMPONENTS
        ModDataComponents.DATA_COMPONENTS.register(modBusGroup);

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Trowel & Error common setup complete.");
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Trowel & Error client setup complete.");
        }
    }
}