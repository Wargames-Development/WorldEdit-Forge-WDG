/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.forge;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.registry.BlockRegistryNameCompleter;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.LegacyBlockRegistry;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Forge-aware block registry that preserves legacy parsing while exposing
 * strict registry-name identity resolution.
 */
class ForgeBlockRegistry extends LegacyBlockRegistry implements BlockRegistryNameResolver, BlockRegistryNameCompleter {

    private final RegistryAccess registry;
    private volatile RegistrySnapshot registrySnapshot;

    ForgeBlockRegistry() {
        this(new ActiveRegistryAccess());
    }

    ForgeBlockRegistry(RegistryAccess registry) {
        this.registry = registry;
    }

    @Nullable
    @Override
    public BaseBlock createFromId(String id) {
        Integer blockId = getBlockId(id);
        if (blockId != null) {
            return createFromId(blockId);
        }
        return super.createFromId(id);
    }

    @Nullable
    @Override
    public String getRegistryName(int blockId) {
        Object block = registry.getById(blockId);
        if (block == null || registry.getId(block) != blockId) {
            return null;
        }

        String registryName = registry.getName(block);
        if (!isValidRegistryName(registryName)) {
            return null;
        }

        return registry.getByName(registryName) == block ? registryName : null;
    }

    @Nullable
    @Override
    public Integer getBlockId(String registryName) {
        if (!isValidRegistryName(registryName)) {
            return null;
        }

        Object block = registry.getByName(registryName);
        if (block == null || !registryName.equals(registry.getName(block))) {
            return null;
        }

        int blockId = registry.getId(block);
        if (blockId < 0 || registry.getById(blockId) != block) {
            return null;
        }

        return blockId;
    }

    @Override
    public List<String> getRegistryNameSuggestions(String prefix, int limit) {
        if (!isValidRegistryPrefix(prefix) || limit <= 0) {
            return Collections.emptyList();
        }

        RegistrySnapshot snapshot = getRegistrySnapshot();
        String searchPrefix = prefix.toLowerCase(Locale.ROOT);
        int index = Collections.binarySearch(snapshot.searchNames, searchPrefix);
        if (index < 0) {
            index = -index - 1;
        } else {
            while (index > 0 && snapshot.searchNames.get(index - 1).equals(searchPrefix)) {
                index--;
            }
        }

        List<String> suggestions = new ArrayList<String>();
        while (index < snapshot.searchNames.size() && suggestions.size() < limit) {
            String searchName = snapshot.searchNames.get(index);
            if (!searchName.startsWith(searchPrefix)) {
                break;
            }
            suggestions.add(snapshot.registryNames.get(index));
            index++;
        }

        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(suggestions);
    }

    private RegistrySnapshot getRegistrySnapshot() {
        RegistrySnapshot snapshot = registrySnapshot;
        if (snapshot == null) {
            synchronized (this) {
                snapshot = registrySnapshot;
                if (snapshot == null) {
                    snapshot = buildRegistrySnapshot();
                    registrySnapshot = snapshot;
                }
            }
        }
        return snapshot;
    }

    private RegistrySnapshot buildRegistrySnapshot() {
        Map<String, String> namesBySearchName = new TreeMap<String, String>();
        for (Object block : registry.getRegisteredBlocks()) {
            String registryName = registry.getName(block);
            if (!isValidRegistryName(registryName)) {
                continue;
            }

            int blockId = registry.getId(block);
            if (blockId < 0 || registry.getById(blockId) != block
                    || registry.getByName(registryName) != block) {
                continue;
            }

            String searchName = registryName.toLowerCase(Locale.ROOT);
            String existing = namesBySearchName.get(searchName);
            if (existing == null || registryName.compareTo(existing) < 0) {
                namesBySearchName.put(searchName, registryName);
            }
        }

        List<String> searchNames = new ArrayList<String>(namesBySearchName.size());
        List<String> registryNames = new ArrayList<String>(namesBySearchName.size());
        for (Map.Entry<String, String> entry : namesBySearchName.entrySet()) {
            searchNames.add(entry.getKey());
            registryNames.add(entry.getValue());
        }
        return new RegistrySnapshot(searchNames, registryNames);
    }

    private static boolean isValidRegistryPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || prefix.indexOf(':') != prefix.lastIndexOf(':')) {
            return false;
        }

        int separator = prefix.indexOf(':');
        if (separator < 0) {
            return isValidPart(prefix, 0, prefix.length(), false);
        }
        if (separator == 0) {
            return false;
        }
        return isValidPart(prefix, 0, separator, false)
                && isValidPart(prefix, separator + 1, prefix.length(), true);
    }

    private static boolean isValidRegistryName(String registryName) {
        if (registryName == null) {
            return false;
        }

        int separator = registryName.indexOf(':');
        if (separator <= 0 || separator != registryName.lastIndexOf(':')
                || separator == registryName.length() - 1) {
            return false;
        }

        return isValidPart(registryName, 0, separator, false)
                && isValidPart(registryName, separator + 1, registryName.length(), true);
    }

    private static boolean isValidPart(String value, int start, int end, boolean allowSlash) {
        for (int i = start; i < end; i++) {
            char character = value.charAt(i);
            if ((character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '_'
                    || character == '-'
                    || character == '.'
                    || (allowSlash && character == '/')) {
                continue;
            }
            return false;
        }
        return true;
    }

    interface RegistryAccess {

        @Nullable
        Object getById(int blockId);

        @Nullable
        Object getByName(String registryName);

        @Nullable
        String getName(Object block);

        int getId(Object block);

        Iterable<?> getRegisteredBlocks();

    }

    private static class ActiveRegistryAccess implements RegistryAccess {

        private FMLControlledNamespacedRegistry getRegistry() {
            return GameData.getBlockRegistry();
        }

        @Nullable
        @Override
        public Object getById(int blockId) {
            return getRegistry().getRaw(blockId);
        }

        @Nullable
        @Override
        public Object getByName(String registryName) {
            return getRegistry().getRaw(registryName);
        }

        @Nullable
        @Override
        public String getName(Object block) {
            return getRegistry().getNameForObject((Block) block);
        }

        @Override
        public int getId(Object block) {
            return getRegistry().getId((Block) block);
        }

        @Override
        public Iterable<?> getRegisteredBlocks() {
            return getRegistry().typeSafeIterable();
        }

    }

    private static class RegistrySnapshot {

        private final List<String> searchNames;
        private final List<String> registryNames;

        RegistrySnapshot(List<String> searchNames, List<String> registryNames) {
            this.searchNames = Collections.unmodifiableList(new ArrayList<String>(searchNames));
            this.registryNames = Collections.unmodifiableList(new ArrayList<String>(registryNames));
        }

    }

}
