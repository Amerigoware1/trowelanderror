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

package com.trowelanderror.history;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class BlockChangeAdapter implements JsonSerializer<BlockChange>, JsonDeserializer<BlockChange> {
    @Override
    public JsonElement serialize(BlockChange src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        // Pos
        JsonArray posArr = new JsonArray();
        posArr.add(src.getPos().getX());
        posArr.add(src.getPos().getY());
        posArr.add(src.getPos().getZ());
        obj.add("pos", posArr);
        // BlockState
        BlockState state = src.getOldState();
        obj.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        JsonObject props = new JsonObject();
        for (Property<?> property : state.getProperties()) {
            props.addProperty(property.getName(), state.getValue(property).toString());
        }
        obj.add("properties", props);
        return obj;
    }

    @Override
    public BlockChange deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        // Pos
        JsonArray posArr = obj.get("pos").getAsJsonArray();
        BlockPos pos = new BlockPos(
                posArr.get(0).getAsInt(),
                posArr.get(1).getAsInt(),
                posArr.get(2).getAsInt()
        );

        // BlockState
        String blockId = obj.get("block").getAsString();

        // ★ Fixed line
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
        if (block == null) {
            throw new JsonParseException("Unknown block: " + blockId);
        }

        BlockState state = block.defaultBlockState();

        JsonObject props = obj.get("properties").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : props.entrySet()) {
            String propName = entry.getKey();
            String value = entry.getValue().getAsString();
            Property<?> property = state.getBlock().getStateDefinition().getProperty(propName);
            if (property != null) {
                state = setProperty(state, property, value);
            }
        }

        return new BlockChange(pos, state);
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow(() -> new IllegalArgumentException("Invalid value: " + value)));
    }
}
