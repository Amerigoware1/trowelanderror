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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trowelanderror.TrowelAndError;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class HistoryManager {
    private static final int MAX_ENTRIES = 10;
    private static final String FILE_NAME = "trowelanderror_history.json";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(BlockChange.class, new BlockChangeAdapter())
            .setPrettyPrinting()
            .create();

    private final Path historyFile;
    private final LinkedList<ActionEntry> history = new LinkedList<>();

    private static HistoryManager instance;

    public static HistoryManager getInstance(MinecraftServer server) {
        if (instance == null) {
            instance = new HistoryManager(server);
        }
        return instance;
    }

    private HistoryManager(MinecraftServer server) {
        // Correct path for 1.21+
        Path saveDir = server.getWorldPath(LevelResource.ROOT);
        this.historyFile = saveDir.resolve(FILE_NAME);
        load();
    }

    public void recordAction(String trowelType, List<BlockChange> changes) {
        if (changes.isEmpty()) return;

        ActionEntry entry = new ActionEntry(trowelType, changes);
        synchronized (history) {
            history.add(entry);
            if (history.size() > MAX_ENTRIES) {
                history.removeFirst();
            }
            save();
        }
    }

    public ActionEntry getLastAction() {
        synchronized (history) {
            return history.isEmpty() ? null : history.getLast();
        }
    }

    public void removeLastAction() {
        synchronized (history) {
            if (!history.isEmpty()) {
                history.removeLast();
                save();
            }
        }
    }

    private void load() {
        if (!Files.exists(historyFile)) return;

        try (Reader reader = Files.newBufferedReader(historyFile)) {
            ActionEntry[] entries = GSON.fromJson(reader, ActionEntry[].class);
            if (entries == null) return;

            synchronized (history) {
                history.clear();
                for (ActionEntry e : entries) {
                    history.add(e);
                }
                while (history.size() > MAX_ENTRIES) {
                    history.removeFirst();
                }
            }
        } catch (IOException e) {
            TrowelAndError.LOGGER.error("Failed to load history", e);
        }
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(historyFile)) {
            GSON.toJson(history.toArray(new ActionEntry[0]), writer);
        } catch (IOException e) {
            TrowelAndError.LOGGER.error("Failed to save history", e);
        }
    }

    // Optional debug helper
    public int getHistorySize() {
        synchronized (history) {
            return history.size();
        }
    }
}
