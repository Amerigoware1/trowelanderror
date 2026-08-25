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

package com.trowelanderror.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DiggerSettings(Shape shape, int diameter, int depth, boolean spareOres) {

    public static final DiggerSettings DEFAULT = new DiggerSettings(Shape.SQUARE, 3, 1, false);

    public enum Shape {
        SQUARE, CIRCLE, DIAMOND;
        public static final Codec<Shape> CODEC = Codec.STRING.xmap(Shape::valueOf, Shape::name);
    }

    public static final Codec<DiggerSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Shape.CODEC.fieldOf("shape").forGetter(DiggerSettings::shape),
            Codec.INT.fieldOf("diameter").forGetter(DiggerSettings::diameter),
            Codec.INT.fieldOf("depth").forGetter(DiggerSettings::depth),
            Codec.BOOL.optionalFieldOf("spare_ores", false).forGetter(DiggerSettings::spareOres)
    ).apply(inst, DiggerSettings::new));
}