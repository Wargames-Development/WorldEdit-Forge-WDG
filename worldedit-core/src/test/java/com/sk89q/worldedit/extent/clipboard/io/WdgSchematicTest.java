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

package com.sk89q.worldedit.extent.clipboard.io;

import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.EntityRegistry;
import com.sk89q.worldedit.world.registry.ItemRegistry;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.NullBiomeRegistry;
import com.sk89q.worldedit.world.registry.NullEntityRegistry;
import com.sk89q.worldedit.world.registry.NullItemRegistry;
import com.sk89q.worldedit.world.registry.State;
import com.sk89q.worldedit.world.registry.WorldData;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Focused tests for WDG schematic version 1.
 */
public class WdgSchematicTest {

    private static final String AIR = "minecraft:air";
    private static final String STONE = "minecraft:stone";
    private static final String WOOL = "minecraft:wool";
    private static final String MACHINE = "testmod:machine";

    @Test
    public void testAliasesExtensionsAndRootDetectionStaySeparate() throws Exception {
        assertSame(ClipboardFormat.WDG_SCHEMATIC, ClipboardFormat.findByAlias("wdg"));
        assertSame(ClipboardFormat.WDG_SCHEMATIC, ClipboardFormat.findByAlias("wdgschem"));
        assertSame(ClipboardFormat.SCHEMATIC, ClipboardFormat.findByAlias("schematic"));
        assertEquals("wdgschem", ClipboardFormat.WDG_SCHEMATIC.getPrimaryExtension());
        assertEquals("schematic", ClipboardFormat.SCHEMATIC.getPrimaryExtension());
        assertTrue(WdgSchematicFormat.isValidRegistryName("ExampleMod:MachineBlock"));

        File wdg = writeTemporary(writeClipboard(createSimpleClipboard(), sourceWorldData()));
        File legacy = writeTemporary(writeNamedCompound("Schematic", new LinkedHashMap<String, Tag>()));
        File truncated = writeTemporary(new byte[] {1, 2, 3, 4});
        try {
            assertTrue(ClipboardFormat.WDG_SCHEMATIC.isFormat(wdg));
            assertFalse(ClipboardFormat.SCHEMATIC.isFormat(wdg));
            assertSame(ClipboardFormat.WDG_SCHEMATIC, ClipboardFormat.findByFile(wdg));

            assertTrue(ClipboardFormat.SCHEMATIC.isFormat(legacy));
            assertFalse(ClipboardFormat.WDG_SCHEMATIC.isFormat(legacy));
            assertSame(ClipboardFormat.SCHEMATIC, ClipboardFormat.findByFile(legacy));

            assertFalse(ClipboardFormat.WDG_SCHEMATIC.isFormat(truncated));
            assertFalse(ClipboardFormat.SCHEMATIC.isFormat(truncated));
        } finally {
            assertTrue(wdg.delete());
            assertTrue(legacy.delete());
            assertTrue(truncated.delete());
        }
    }

    @Test
    public void testRegistryRemapMetadataGeometryTileAndEntityRoundTrip() throws Exception {
        Vector minimum = new Vector(10, 20, 30);
        CuboidRegion region = new CuboidRegion(minimum, minimum.add(4, 0, 0));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(new Vector(8, 19, 25));
        clipboard.setBlock(minimum.add(0, 0, 0), new BaseBlock(0, 0));
        clipboard.setBlock(minimum.add(1, 0, 0), new BaseBlock(1, 1));
        clipboard.setBlock(minimum.add(2, 0, 0), new BaseBlock(35, 7));
        clipboard.setBlock(minimum.add(3, 0, 0), new BaseBlock(35, 14));
        clipboard.setBlock(minimum.add(4, 0, 0), new BaseBlock(300, 15, createMachineNbt()));

        Map<String, Tag> entityNbt = new LinkedHashMap<String, Tag>();
        entityNbt.put("CustomName", new StringTag("WDG Test Entity"));
        clipboard.createEntity(new Location(clipboard, minimum.add(2.25, 0.5, 0.75), 37.5F, -12.25F),
                new BaseEntity("Pig", new CompoundTag(entityNbt)));

        byte[] data = writeClipboard(clipboard, sourceWorldData());
        CompoundTag root = readRoot(data);
        assertEquals(WdgSchematicFormat.VERSION, root.getInt("Version"));
        assertArrayEquals(new int[] {0, 1, 2, 2, 3}, root.getIntArray("Blocks"));
        assertArrayEquals(new byte[] {0, 1, 7, 14, 15}, root.getByteArray("Data"));
        assertEquals(Arrays.asList(AIR, STONE, WOOL, MACHINE), readPalette(root));
        assertFalse(root.getValue().containsKey("AddBlocks"));

        TestRegistry destinationRegistry = new TestRegistry()
                .register(0, AIR)
                .register(1, STONE)
                .register(35, WOOL)
                .register(812, MACHINE);
        Clipboard loaded = readClipboard(data, new TestWorldData(destinationRegistry));

        assertEquals(region.getMinimumPoint(), loaded.getMinimumPoint());
        assertEquals(region.getMaximumPoint(), loaded.getMaximumPoint());
        assertEquals(new Vector(8, 19, 25), loaded.getOrigin());
        assertEquals(812, loaded.getBlock(minimum.add(4, 0, 0)).getId());
        assertEquals(15, loaded.getBlock(minimum.add(4, 0, 0)).getData());
        assertEquals(14, loaded.getBlock(minimum.add(3, 0, 0)).getData());

        CompoundTag tile = loaded.getBlock(minimum.add(4, 0, 0)).getNbtData();
        assertEquals("testmod.machine", tile.getString("id"));
        assertEquals("Configured", tile.getString("CustomName"));
        assertEquals(42, tile.getInt("CookTime"));
        assertArrayEquals(new byte[] {3, 1, 4}, tile.getByteArray("Bytes"));
        assertArrayEquals(new int[] {9, 2, 6}, tile.getIntArray("Ints"));
        assertEquals("nested-value", ((CompoundTag) tile.getValue().get("MachineConfig")).getString("Mode"));
        assertEquals(2, tile.getListTag("Items").getValue().size());
        assertEquals(4, tile.getInt("x"));
        assertEquals(0, tile.getInt("y"));
        assertEquals(0, tile.getInt("z"));

        assertEquals(1, loaded.getEntities().size());
        Entity loadedEntity = loaded.getEntities().get(0);
        assertEquals("Pig", loadedEntity.getState().getTypeId());
        assertEquals("WDG Test Entity", loadedEntity.getState().getNbtData().getString("CustomName"));
        assertEquals(minimum.add(2.25, 0.5, 0.75), loadedEntity.getLocation().toVector());
        assertEquals(37.5F, loadedEntity.getLocation().getYaw(), 0.0001F);
        assertEquals(-12.25F, loadedEntity.getLocation().getPitch(), 0.0001F);
    }

    @Test
    public void testMissingRegistryNameBecomesAirOnceWithoutTileNbt() throws Exception {
        Vector minimum = new Vector(0, 0, 0);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(minimum, minimum.add(1, 0, 0)));
        clipboard.setBlock(minimum, new BaseBlock(300, 3, createMachineNbt()));
        clipboard.setBlock(minimum.add(1, 0, 0), new BaseBlock(300, 4, createMachineNbt()));
        byte[] data = writeClipboard(clipboard, sourceWorldData());

        WdgSchematicReader reader = newReader(data);
        Clipboard loaded = reader.read(new TestWorldData(new TestRegistry().register(0, AIR)));
        assertEquals(0, loaded.getBlock(minimum).getId());
        assertEquals(0, loaded.getBlock(minimum.add(1, 0, 0)).getId());
        assertEquals(0, loaded.getBlock(minimum).getData());
        assertEquals(0, loaded.getBlock(minimum.add(1, 0, 0)).getData());
        assertNull(loaded.getBlock(minimum).getNbtData());
        assertNull(loaded.getBlock(minimum.add(1, 0, 0)).getNbtData());

        Set<String> missing = reader.getMissingBlockRegistryNames();
        assertEquals(1, missing.size());
        assertTrue(missing.contains(MACHINE));
    }

    @Test
    public void testWriterRejectsUnresolvedIdAndRequiresResolver() throws Exception {
        BlockArrayClipboard clipboard = createSimpleClipboard();
        clipboard.setBlock(clipboard.getMinimumPoint(), new BaseBlock(99, 0));

        expectIOException("block ID 99", new IoRunnable() {
            @Override
            public void run() throws Exception {
                writeClipboard(clipboard, sourceWorldData());
            }
        });

        expectIOException("does not support registry-stable", new IoRunnable() {
            @Override
            public void run() throws Exception {
                writeClipboard(createSimpleClipboard(), LegacyWorldData.getInstance());
            }
        });
    }

    @Test
    public void testReaderRequiresResolverAndRejectsUnsupportedVersion() throws Exception {
        final byte[] valid = writeClipboard(createSimpleClipboard(), sourceWorldData());
        expectIOException("does not support registry-stable", new IoRunnable() {
            @Override
            public void run() throws Exception {
                readClipboard(valid, LegacyWorldData.getInstance());
            }
        });

        CompoundTag root = readRoot(valid);
        Map<String, Tag> values = new LinkedHashMap<String, Tag>(root.getValue());
        values.put("Version", new IntTag(2));
        final byte[] unsupported = writeNamedCompound(WdgSchematicFormat.ROOT_NAME, values);
        expectIOException("Unsupported WDG schematic version 2", new IoRunnable() {
            @Override
            public void run() throws Exception {
                readClipboard(unsupported, destinationWorldData());
            }
        });
    }

    @Test
    public void testMalformedPaletteIndexMetadataAndVolumeFailCleanly() throws Exception {
        CompoundTag root = readRoot(writeClipboard(createSimpleClipboard(), sourceWorldData()));

        Map<String, Tag> invalidIndex = new LinkedHashMap<String, Tag>(root.getValue());
        invalidIndex.put("Blocks", new IntArrayTag(new int[] {99, 1}));
        expectReadFailure("palette index", invalidIndex);

        Map<String, Tag> invalidMetadata = new LinkedHashMap<String, Tag>(root.getValue());
        invalidMetadata.put("Data", new ByteArrayTag(new byte[] {16, 0}));
        expectReadFailure("metadata", invalidMetadata);

        Map<String, Tag> overflow = new LinkedHashMap<String, Tag>(root.getValue());
        overflow.put("Width", new IntTag(Integer.MAX_VALUE));
        overflow.put("Height", new IntTag(2));
        overflow.put("Length", new IntTag(2));
        expectReadFailure("volume is too large", overflow);

        Map<String, Tag> longOverflow = new LinkedHashMap<String, Tag>(root.getValue());
        longOverflow.put("Width", new IntTag(Integer.MAX_VALUE));
        longOverflow.put("Height", new IntTag(Integer.MAX_VALUE));
        longOverflow.put("Length", new IntTag(Integer.MAX_VALUE));
        expectReadFailure("volume is too large", longOverflow);

        Map<String, Tag> malformedName = new LinkedHashMap<String, Tag>(root.getValue());
        List<Tag> names = new ArrayList<Tag>();
        names.add(new StringTag("not-a-namespaced-name"));
        malformedName.put("Palette", new ListTag(StringTag.class, names));
        expectReadFailure("complete registry name", malformedName);
    }

    @Test
    public void testPaletteFirstUseOrderAndRepeatedReuse() throws Exception {
        Vector minimum = Vector.ZERO;
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(minimum, minimum.add(3, 0, 0)));
        clipboard.setBlock(minimum, new BaseBlock(35, 14));
        clipboard.setBlock(minimum.add(1, 0, 0), new BaseBlock(1, 0));
        clipboard.setBlock(minimum.add(2, 0, 0), new BaseBlock(35, 1));
        clipboard.setBlock(minimum.add(3, 0, 0), new BaseBlock(300, 2));

        CompoundTag root = readRoot(writeClipboard(clipboard, sourceWorldData()));
        assertEquals(Arrays.asList(WOOL, STONE, MACHINE), readPalette(root));
        assertArrayEquals(new int[] {0, 1, 0, 2}, root.getIntArray("Blocks"));
    }


    @Test
    public void testBlockArrayOrderingMatchesWorldEditCoordinateOrder() throws Exception {
        Vector minimum = Vector.ZERO;
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(minimum, minimum.add(1, 1, 1)));
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int index = y * 4 + z * 2 + x;
                    clipboard.setBlock(new Vector(x, y, z), new BaseBlock(1, index));
                }
            }
        }

        CompoundTag root = readRoot(writeClipboard(clipboard, sourceWorldData()));
        assertArrayEquals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7}, root.getByteArray("Data"));
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0}, root.getIntArray("Blocks"));
    }

    @Test
    public void testLengthRootAndPaletteTypeValidation() throws Exception {
        CompoundTag root = readRoot(writeClipboard(createSimpleClipboard(), sourceWorldData()));

        Map<String, Tag> shortBlocks = new LinkedHashMap<String, Tag>(root.getValue());
        shortBlocks.put("Blocks", new IntArrayTag(new int[] {0}));
        expectReadFailure("Blocks length", shortBlocks);

        Map<String, Tag> shortData = new LinkedHashMap<String, Tag>(root.getValue());
        shortData.put("Data", new ByteArrayTag(new byte[] {0}));
        expectReadFailure("Data length", shortData);

        Map<String, Tag> wrongPaletteType = new LinkedHashMap<String, Tag>(root.getValue());
        List<Tag> integerPalette = new ArrayList<Tag>();
        integerPalette.add(new IntTag(1));
        wrongPaletteType.put("Palette", new ListTag(IntTag.class, integerPalette));
        expectReadFailure("Palette list must contain string tags", wrongPaletteType);

        Map<String, Tag> emptyPalette = new LinkedHashMap<String, Tag>(root.getValue());
        emptyPalette.put("Palette", new ListTag(StringTag.class, new ArrayList<Tag>()));
        expectReadFailure("Palette must contain at least one", emptyPalette);

        final byte[] wrongRoot = writeNamedCompound("Schematic", root.getValue());
        expectIOException("Expected root tag", new IoRunnable() {
            @Override
            public void run() throws Exception {
                readClipboard(wrongRoot, destinationWorldData());
            }
        });
    }

    @Test
    public void testTileEntityValidationRejectsMalformedCoordinatesAndDuplicates() throws Exception {
        CompoundTag root = readRoot(writeClipboard(createSimpleClipboard(), sourceWorldData()));

        Map<String, Tag> outsideValues = new LinkedHashMap<String, Tag>(createMachineNbt().getValue());
        outsideValues.put("x", new IntTag(2));
        outsideValues.put("y", new IntTag(0));
        outsideValues.put("z", new IntTag(0));
        List<Tag> outsideList = new ArrayList<Tag>();
        outsideList.add(new CompoundTag(outsideValues));
        Map<String, Tag> outsideRoot = new LinkedHashMap<String, Tag>(root.getValue());
        outsideRoot.put("TileEntities", new ListTag(CompoundTag.class, outsideList));
        expectReadFailure("coordinates outside the region", outsideRoot);

        Map<String, Tag> validValues = new LinkedHashMap<String, Tag>(createMachineNbt().getValue());
        validValues.put("x", new IntTag(0));
        validValues.put("y", new IntTag(0));
        validValues.put("z", new IntTag(0));
        List<Tag> duplicateList = new ArrayList<Tag>();
        duplicateList.add(new CompoundTag(validValues));
        duplicateList.add(new CompoundTag(new LinkedHashMap<String, Tag>(validValues)));
        Map<String, Tag> duplicateRoot = new LinkedHashMap<String, Tag>(root.getValue());
        duplicateRoot.put("TileEntities", new ListTag(CompoundTag.class, duplicateList));
        expectReadFailure("Duplicate tile entity coordinates", duplicateRoot);

        Map<String, Tag> wrongTypeRoot = new LinkedHashMap<String, Tag>(root.getValue());
        List<Tag> strings = new ArrayList<Tag>();
        strings.add(new StringTag("not-a-compound"));
        wrongTypeRoot.put("TileEntities", new ListTag(StringTag.class, strings));
        expectReadFailure("TileEntities list must contain compound tags", wrongTypeRoot);
    }

    @Test
    public void testEntityValidationRejectsMissingAndMalformedRequiredFields() throws Exception {
        CompoundTag root = readRoot(writeClipboard(createSimpleClipboard(), sourceWorldData()));

        Map<String, Tag> missingPosition = new LinkedHashMap<String, Tag>();
        missingPosition.put("id", new StringTag("Pig"));
        missingPosition.put("Rotation", floatList(0F, 0F));
        List<Tag> missingPositionList = new ArrayList<Tag>();
        missingPositionList.add(new CompoundTag(missingPosition));
        Map<String, Tag> missingPositionRoot = new LinkedHashMap<String, Tag>(root.getValue());
        missingPositionRoot.put("Entities", new ListTag(CompoundTag.class, missingPositionList));
        expectReadFailure("missing a 'Pos' tag", missingPositionRoot);

        Map<String, Tag> malformedPosition = new LinkedHashMap<String, Tag>();
        malformedPosition.put("id", new StringTag("Pig"));
        List<Tag> twoDoubles = new ArrayList<Tag>();
        twoDoubles.add(new DoubleTag(0));
        twoDoubles.add(new DoubleTag(0));
        malformedPosition.put("Pos", new ListTag(DoubleTag.class, twoDoubles));
        malformedPosition.put("Rotation", floatList(0F, 0F));
        List<Tag> malformedPositionList = new ArrayList<Tag>();
        malformedPositionList.add(new CompoundTag(malformedPosition));
        Map<String, Tag> malformedPositionRoot = new LinkedHashMap<String, Tag>(root.getValue());
        malformedPositionRoot.put("Entities", new ListTag(CompoundTag.class, malformedPositionList));
        expectReadFailure("exactly three doubles", malformedPositionRoot);
    }

    private void expectReadFailure(String message, Map<String, Tag> values) throws Exception {
        final byte[] bytes = writeNamedCompound(WdgSchematicFormat.ROOT_NAME, values);
        expectIOException(message, new IoRunnable() {
            @Override
            public void run() throws Exception {
                readClipboard(bytes, destinationWorldData());
            }
        });
    }

    private ListTag floatList(float first, float second) {
        List<Tag> values = new ArrayList<Tag>();
        values.add(new FloatTag(first));
        values.add(new FloatTag(second));
        return new ListTag(FloatTag.class, values);
    }

    private BlockArrayClipboard createSimpleClipboard() throws Exception {
        Vector minimum = new Vector(2, 3, 4);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(minimum, minimum.add(1, 0, 0)));
        clipboard.setOrigin(new Vector(1, 2, 3));
        clipboard.setBlock(minimum, new BaseBlock(0, 0));
        clipboard.setBlock(minimum.add(1, 0, 0), new BaseBlock(1, 0));
        return clipboard;
    }

    private CompoundTag createMachineNbt() {
        Map<String, Tag> nested = new LinkedHashMap<String, Tag>();
        nested.put("Mode", new StringTag("nested-value"));
        nested.put("Enabled", new ByteTag((byte) 1));

        List<Tag> items = new ArrayList<Tag>();
        Map<String, Tag> firstItem = new LinkedHashMap<String, Tag>();
        firstItem.put("Slot", new ByteTag((byte) 0));
        firstItem.put("id", new StringTag("minecraft:iron_ingot"));
        firstItem.put("Count", new ByteTag((byte) 12));
        items.add(new CompoundTag(firstItem));
        Map<String, Tag> secondItem = new LinkedHashMap<String, Tag>();
        secondItem.put("Slot", new ByteTag((byte) 4));
        secondItem.put("id", new StringTag("minecraft:coal"));
        secondItem.put("Count", new ByteTag((byte) 5));
        items.add(new CompoundTag(secondItem));

        Map<String, Tag> values = new LinkedHashMap<String, Tag>();
        values.put("id", new StringTag("testmod.machine"));
        values.put("x", new IntTag(999));
        values.put("y", new IntTag(998));
        values.put("z", new IntTag(997));
        values.put("CustomName", new StringTag("Configured"));
        values.put("CookTime", new IntTag(42));
        values.put("Items", new ListTag(CompoundTag.class, items));
        values.put("MachineConfig", new CompoundTag(nested));
        values.put("Bytes", new ByteArrayTag(new byte[] {3, 1, 4}));
        values.put("Ints", new IntArrayTag(new int[] {9, 2, 6}));
        return new CompoundTag(values);
    }

    private TestWorldData sourceWorldData() {
        return new TestWorldData(new TestRegistry()
                .register(0, AIR)
                .register(1, STONE)
                .register(35, WOOL)
                .register(300, MACHINE));
    }

    private TestWorldData destinationWorldData() {
        return new TestWorldData(new TestRegistry()
                .register(0, AIR)
                .register(1, STONE)
                .register(35, WOOL)
                .register(812, MACHINE));
    }

    private byte[] writeClipboard(Clipboard clipboard, WorldData worldData) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ClipboardWriter writer = ClipboardFormat.WDG_SCHEMATIC.getWriter(output);
        try {
            writer.write(clipboard, worldData);
        } finally {
            writer.close();
        }
        return output.toByteArray();
    }

    private Clipboard readClipboard(byte[] data, WorldData worldData) throws Exception {
        return newReader(data).read(worldData);
    }

    private WdgSchematicReader newReader(byte[] data) throws Exception {
        return (WdgSchematicReader) ClipboardFormat.WDG_SCHEMATIC.getReader(
                new ByteArrayInputStream(data));
    }

    private CompoundTag readRoot(byte[] data) throws Exception {
        NBTInputStream input = new NBTInputStream(new GZIPInputStream(new ByteArrayInputStream(data)));
        try {
            NamedTag namedTag = input.readNamedTag();
            assertEquals(WdgSchematicFormat.ROOT_NAME, namedTag.getName());
            return (CompoundTag) namedTag.getTag();
        } finally {
            input.close();
        }
    }

    private List<String> readPalette(CompoundTag root) {
        List<String> result = new ArrayList<String>();
        for (Tag tag : root.getListTag("Palette").getValue()) {
            result.add(((StringTag) tag).getValue());
        }
        return result;
    }

    private byte[] writeNamedCompound(String name, Map<String, Tag> values) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NBTOutputStream nbt = new NBTOutputStream(new GZIPOutputStream(output));
        try {
            nbt.writeNamedTag(name, new CompoundTag(values));
        } finally {
            nbt.close();
        }
        return output.toByteArray();
    }

    private File writeTemporary(byte[] bytes) throws Exception {
        File file = File.createTempFile("wdg-schematic-test-", ".dat");
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(bytes);
        } finally {
            output.close();
        }
        return file;
    }

    private void expectIOException(String expectedMessage, IoRunnable runnable) throws Exception {
        try {
            runnable.run();
            fail("Expected IOException containing: " + expectedMessage);
        } catch (IOException e) {
            assertTrue("Expected message to contain '" + expectedMessage + "' but was '"
                    + e.getMessage() + "'", e.getMessage().contains(expectedMessage));
        }
    }

    private interface IoRunnable {
        void run() throws Exception;
    }

    private static class TestWorldData implements WorldData {
        private final BlockRegistry blockRegistry;
        private final ItemRegistry itemRegistry = new NullItemRegistry();
        private final EntityRegistry entityRegistry = new NullEntityRegistry();
        private final BiomeRegistry biomeRegistry = new NullBiomeRegistry();

        TestWorldData(BlockRegistry blockRegistry) {
            this.blockRegistry = blockRegistry;
        }

        @Override
        public BlockRegistry getBlockRegistry() {
            return blockRegistry;
        }

        @Override
        public ItemRegistry getItemRegistry() {
            return itemRegistry;
        }

        @Override
        public EntityRegistry getEntityRegistry() {
            return entityRegistry;
        }

        @Override
        public BiomeRegistry getBiomeRegistry() {
            return biomeRegistry;
        }
    }

    private static class TestRegistry implements BlockRegistry, BlockRegistryNameResolver {
        private final Map<Integer, String> namesById = new HashMap<Integer, String>();
        private final Map<String, Integer> idsByName = new HashMap<String, Integer>();

        TestRegistry register(int id, String name) {
            namesById.put(id, name);
            idsByName.put(name, id);
            return this;
        }

        @Nullable
        @Override
        public String getRegistryName(int blockId) {
            return namesById.get(blockId);
        }

        @Nullable
        @Override
        public Integer getBlockId(String registryName) {
            return idsByName.get(registryName);
        }

        @Nullable
        @Override
        public BaseBlock createFromId(String id) {
            Integer blockId = idsByName.get(id);
            return blockId == null ? null : new BaseBlock(blockId);
        }

        @Nullable
        @Override
        public BaseBlock createFromId(int id) {
            return namesById.containsKey(id) ? new BaseBlock(id) : null;
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
