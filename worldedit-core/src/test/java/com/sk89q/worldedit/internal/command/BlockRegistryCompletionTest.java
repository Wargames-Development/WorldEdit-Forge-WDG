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

package com.sk89q.worldedit.internal.command;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistryNameCompleter;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.State;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlockRegistryCompletionTest {

    private FakeCompletingRegistry registry;

    @Before
    public void setUp() {
        registry = new FakeCompletingRegistry(Arrays.asList(
                "examplemod:machine",
                "examplemod:machine_casing",
                "minecraft:dirt",
                "minecraft:stained_glass",
                "minecraft:stained_hardened_clay",
                "minecraft:stone",
                "minecraft:stone_slab",
                "minecraft:wool"));
    }

    @Test
    public void completesPlainRegistryPrefix() {
        assertEquals(Arrays.asList(
                "minecraft:stained_glass",
                "minecraft:stained_hardened_clay",
                "minecraft:stone",
                "minecraft:stone_slab"),
                BlockRegistryCompletion.getPatternSuggestions(registry, "minecraft:st", 100));
    }

    @Test
    public void preservesPercentagePrefix() {
        assertEquals(Arrays.asList(
                "50%minecraft:stained_glass",
                "50%minecraft:stained_hardened_clay",
                "50%minecraft:stone",
                "50%minecraft:stone_slab"),
                BlockRegistryCompletion.getPatternSuggestions(registry, "50%minecraft:st", 100));
    }

    @Test
    public void preservesPreviousCommaSeparatedEntries() {
        assertEquals(Collections.singletonList("minecraft:stone,minecraft:dirt"),
                BlockRegistryCompletion.getPatternSuggestions(
                        registry, "minecraft:stone,minecraft:di", 100));
        assertEquals(Arrays.asList(
                "25%minecraft:stone,75%examplemod:machine",
                "25%minecraft:stone,75%examplemod:machine_casing"),
                BlockRegistryCompletion.getPatternSuggestions(
                        registry, "25%minecraft:stone,75%examplemod:mach", 100));
    }

    @Test
    public void completesMetadataWithoutReplacingBlockName() {
        assertEquals(Arrays.asList(
                "minecraft:wool:1",
                "minecraft:wool:10",
                "minecraft:wool:11",
                "minecraft:wool:12",
                "minecraft:wool:13",
                "minecraft:wool:14",
                "minecraft:wool:15"),
                BlockRegistryCompletion.getPatternSuggestions(
                        registry, "minecraft:wool:1", 100));
        assertEquals(Arrays.asList(
                "wool:0",
                "wool:1",
                "wool:2",
                "wool:3"),
                BlockRegistryCompletion.getPatternSuggestions(registry, "wool:", 4));
        assertEquals(Arrays.asList(
                "50%minecraft:wool:1",
                "50%minecraft:wool:10",
                "50%minecraft:wool:11"),
                BlockRegistryCompletion.getPatternSuggestions(
                        registry, "50%minecraft:wool:1", 3));
    }

    @Test
    public void rejectsInvalidMetadataAndUnsafeComplexSyntax() {
        assertTrue(BlockRegistryCompletion.getPatternSuggestions(
                registry, "minecraft:wool:16", 100).isEmpty());
        assertTrue(BlockRegistryCompletion.getPatternSuggestions(
                registry, "minecraft:missing:1", 100).isEmpty());
        assertTrue(BlockRegistryCompletion.getPatternSuggestions(
                registry, "#minecraft:st", 100).isEmpty());
        assertTrue(BlockRegistryCompletion.getPatternSuggestions(
                registry, "minecraft:stone&minecraft:di", 100).isEmpty());
        assertTrue(BlockRegistryCompletion.getPatternSuggestions(
                registry, "", 100).isEmpty());
    }

    @Test
    public void blockCompletionRejectsPatternOperatorsAndHonoursLimit() {
        assertEquals(Collections.singletonList("minecraft:stone"),
                BlockRegistryCompletion.getBlockSuggestions(registry, "minecraft:sto", 1));
        assertTrue(BlockRegistryCompletion.getBlockSuggestions(
                registry, "50%minecraft:st", 100).isEmpty());
        assertTrue(BlockRegistryCompletion.getBlockSuggestions(
                registry, "missing:", 100).isEmpty());
    }

    private static class FakeCompletingRegistry implements BlockRegistry,
            BlockRegistryNameCompleter, BlockRegistryNameResolver {

        private final List<String> names;
        private final Map<String, Integer> ids = new HashMap<String, Integer>();

        FakeCompletingRegistry(List<String> names) {
            this.names = new ArrayList<String>(names);
            Collections.sort(this.names);
            for (int i = 0; i < this.names.size(); i++) {
                ids.put(this.names.get(i), i + 1);
            }
        }

        @Override
        public List<String> getRegistryNameSuggestions(String prefix, int limit) {
            if (prefix == null || prefix.isEmpty() || limit <= 0) {
                return Collections.emptyList();
            }

            String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<String>();
            for (String name : names) {
                if (name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                    suggestions.add(name);
                    if (suggestions.size() == limit) {
                        break;
                    }
                }
            }
            return suggestions;
        }

        @Nullable
        @Override
        public String getRegistryName(int blockId) {
            for (Map.Entry<String, Integer> entry : ids.entrySet()) {
                if (entry.getValue() == blockId) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Nullable
        @Override
        public Integer getBlockId(String registryName) {
            return ids.get(registryName);
        }

        @Nullable
        @Override
        public BaseBlock createFromId(String id) {
            return null;
        }

        @Nullable
        @Override
        public BaseBlock createFromId(int id) {
            return null;
        }

        @Nullable
        @Override
        public BlockMaterial getMaterial(BaseBlock block) {
            return null;
        }

        @Nullable
        @Override
        public Map<String, ? extends State> getStates(BaseBlock block) {
            return null;
        }

    }

}
