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
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ForgeBlockRegistryTest {

    private FakeRegistryAccess registryAccess;
    private ForgeBlockRegistry registry;

    @Before
    public void setUp() {
        registryAccess = new FakeRegistryAccess();
        registryAccess.register(0, "minecraft:air");
        registryAccess.register(1, "minecraft:stone");
        registryAccess.register(35, "minecraft:wool");
        registryAccess.register(250, "examplemod:machine_block");
        registryAccess.register(251, "hbm:tile.machine_difurnace_off");
        registryAccess.register(252, "ExampleMod:CaseSensitive_Block");
        registryAccess.register(253, "examplemod:casesensitive_block");
        registryAccess.register(254, "invalid registry name");
        registryAccess.register(255, "minecraft:wool:14");
        registryAccess.alias("minecraft:rock", "minecraft:stone");
        registry = new ForgeBlockRegistry(registryAccess);
    }

    @Test
    public void exposesOptionalResolverCapability() {
        assertTrue(registry instanceof BlockRegistryNameResolver);
        assertTrue(registry instanceof BlockRegistryNameCompleter);
    }


    @Test
    public void registrySuggestionsAreSortedCanonicalAndCaseInsensitive() {
        assertEquals(Arrays.asList(
                "minecraft:air",
                "minecraft:stone",
                "minecraft:wool"), registry.getRegistryNameSuggestions("mine", 100));
        assertEquals(Collections.singletonList("minecraft:stone"),
                registry.getRegistryNameSuggestions("MINECRAFT:ST", 100));
        assertEquals(Collections.singletonList("ExampleMod:CaseSensitive_Block"),
                registry.getRegistryNameSuggestions("examplemod:case", 100));
        assertEquals(Collections.singletonList("minecraft:wool"),
                registry.getRegistryNameSuggestions("minecraft:wool", 100));
    }

    @Test
    public void registrySuggestionsRejectEmptyMalformedAndUnknownPrefixes() {
        assertTrue(registry.getRegistryNameSuggestions("", 100).isEmpty());
        assertTrue(registry.getRegistryNameSuggestions(":stone", 100).isEmpty());
        assertTrue(registry.getRegistryNameSuggestions("minecraft:stone:1", 100).isEmpty());
        assertTrue(registry.getRegistryNameSuggestions("missing:", 100).isEmpty());
        assertEquals(1, registryAccess.getEnumerationCount());
    }

    @Test
    public void registrySnapshotBuildsOnceAndResultsAreSafelyImmutable() {
        List<String> first = registry.getRegistryNameSuggestions("m", 2);
        List<String> second = registry.getRegistryNameSuggestions("minecraft:", 100);

        assertEquals(1, registryAccess.getEnumerationCount());
        assertNotSame(first, second);
        assertEquals(2, first.size());

        try {
            first.add("minecraft:dirt");
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Suggestion result must be immutable");
    }

    @Test
    public void largeRegistryUsesOneSnapshotAndEnforcesLimit() {
        FakeRegistryAccess largeAccess = new FakeRegistryAccess();
        for (int i = 0; i < 5000; i++) {
            largeAccess.register(i, String.format("large:entry_%04d", i));
        }
        ForgeBlockRegistry largeRegistry = new ForgeBlockRegistry(largeAccess);

        assertEquals(25, largeRegistry.getRegistryNameSuggestions("large:entry_0", 25).size());
        assertEquals(25, largeRegistry.getRegistryNameSuggestions("large:entry_0", 25).size());
        assertEquals(1, largeAccess.getEnumerationCount());
    }

    @Test
    public void resolvesVanillaAndModdedRoundTrips() {
        assertRoundTrip(1, "minecraft:stone");
        assertRoundTrip(250, "examplemod:machine_block");
        assertRoundTrip(251, "hbm:tile.machine_difurnace_off");
    }

    @Test
    public void preservesRegistryCaseWithoutNormalising() {
        assertRoundTrip(252, "ExampleMod:CaseSensitive_Block");
        assertRoundTrip(253, "examplemod:casesensitive_block");
        assertNull(registry.getBlockId("examplemod:CASESENSITIVE_BLOCK"));
    }

    @Test
    public void rejectsUnknownIdsAndNamesWithoutAirFallback() {
        assertNull(registry.getRegistryName(-1));
        assertNull(registry.getRegistryName(4095));
        assertNull(registry.getBlockId("wdg_missing:not_a_real_block"));
        assertFalse(Integer.valueOf(0).equals(registry.getBlockId("wdg_missing:not_a_real_block")));
    }

    @Test
    public void rejectsNullEmptyNumericAndMalformedNames() {
        assertNull(registry.getBlockId(null));
        assertNull(registry.getBlockId(""));
        assertNull(registry.getBlockId("   "));
        assertNull(registry.getBlockId("stone"));
        assertNull(registry.getBlockId("1"));
        assertNull(registry.getBlockId("minecraft:"));
        assertNull(registry.getBlockId(":stone"));
        assertNull(registry.getBlockId("minecraft::stone"));
        assertNull(registry.getBlockId("minecraft:stone:1"));
        assertNull(registry.getBlockId("minecraft:stone block"));
        assertNull(registry.getBlockId("minecraft:stone@variant"));
    }

    @Test
    public void strictLookupRejectsRegistryAliasesAndBundledFallback() {
        assertNull(registry.getBlockId("minecraft:rock"));
        assertNull(registry.getBlockId("minecraft:dirt"));

        BaseBlock bundledBlock = registry.createFromId("minecraft:dirt");
        assertNotNull(bundledBlock);
        assertEquals(3, bundledBlock.getId());
    }

    @Test
    public void permissiveCreationUsesExactRegistryNameBeforeLegacyFallback() {
        BaseBlock block = registry.createFromId("examplemod:machine_block");
        assertNotNull(block);
        assertEquals(250, block.getId());
    }

    @Test
    public void metadataRemainsSeparateFromRegistryIdentity() {
        Integer blockId = registry.getBlockId("minecraft:wool");
        assertNotNull(blockId);

        BaseBlock redWool = new BaseBlock(blockId, 14);
        assertEquals(35, redWool.getId());
        assertEquals(14, redWool.getData());
        assertEquals("minecraft:wool", registry.getRegistryName(redWool.getId()));
    }

    private void assertRoundTrip(int blockId, String registryName) {
        assertEquals(registryName, registry.getRegistryName(blockId));
        assertEquals(Integer.valueOf(blockId), registry.getBlockId(registryName));
        assertSame(registryAccess.getById(blockId), registryAccess.getByName(registryName));
    }

    private static class FakeRegistryAccess implements ForgeBlockRegistry.RegistryAccess {

        private final Map<Integer, Object> blocksById = new HashMap<Integer, Object>();
        private final Map<String, Object> blocksByName = new HashMap<String, Object>();
        private final Map<Object, Integer> idsByBlock = new HashMap<Object, Integer>();
        private final Map<Object, String> namesByBlock = new HashMap<Object, String>();
        private int enumerationCount;

        void register(int blockId, String registryName) {
            Object block = new Object();
            blocksById.put(blockId, block);
            blocksByName.put(registryName, block);
            idsByBlock.put(block, blockId);
            namesByBlock.put(block, registryName);
        }

        void alias(String alias, String registryName) {
            blocksByName.put(alias, blocksByName.get(registryName));
        }

        @Override
        public Object getById(int blockId) {
            return blocksById.get(blockId);
        }

        @Override
        public Object getByName(String registryName) {
            return blocksByName.get(registryName);
        }

        @Override
        public String getName(Object block) {
            return namesByBlock.get(block);
        }

        @Override
        public int getId(Object block) {
            Integer blockId = idsByBlock.get(block);
            return blockId == null ? -1 : blockId;
        }

        @Override
        public Iterable<?> getRegisteredBlocks() {
            enumerationCount++;
            return new ArrayList<Object>(blocksById.values());
        }

        int getEnumerationCount() {
            return enumerationCount;
        }

    }

}
