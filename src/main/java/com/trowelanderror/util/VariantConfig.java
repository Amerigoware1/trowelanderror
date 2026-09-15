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

package com.trowelanderror.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class VariantConfig {

    // Prevent null-pointer crashes before load() runs
    private static Map<String, List<String>> variantMap = new HashMap<>();

    public static void load(InputStream jsonStream) {
        Gson gson = new Gson();
        JsonObject root = gson.fromJson(new InputStreamReader(jsonStream), JsonObject.class);
        JsonObject variantsObj = root.getAsJsonObject("variants");

        variantMap = variantsObj.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            JsonArray arr = e.getValue().getAsJsonArray();
                            List<String> list = new ArrayList<>(arr.size());
                            arr.forEach(je -> list.add(je.getAsString()));
                            return list;
                        }
                ));
    }

    public static List<BlockState> getVariantStates(Block base) {
        String baseId = BuiltInRegistries.BLOCK.getKey(base).toString();
        List<String> variantIds = variantMap.getOrDefault(baseId, Collections.singletonList(baseId));

        return variantIds.stream()
                .map(ResourceLocation::parse)
                .map(BuiltInRegistries.BLOCK::get) // Returns Block directly in 1.21.1
                .map(Block::defaultBlockState)
                .toList();
    }

    public static List<Block> getVariantBlocks(Block base) {
        String baseId = BuiltInRegistries.BLOCK.getKey(base).toString();
        List<String> variantIds = variantMap.getOrDefault(baseId, Collections.singletonList(baseId));

        return variantIds.stream()
                .map(ResourceLocation::parse)
                .map(BuiltInRegistries.BLOCK::get) // Returns Block directly in 1.21.1
                .toList();
    }
}

