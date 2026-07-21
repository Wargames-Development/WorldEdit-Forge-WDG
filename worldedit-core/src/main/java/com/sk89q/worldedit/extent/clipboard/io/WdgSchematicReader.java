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
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.WorldData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads WDG registry-stable schematic version 1 files.
 */
public class WdgSchematicReader implements ClipboardReader {

    private final NBTInputStream inputStream;
    private final LinkedHashSet<String> missingBlockRegistryNames = new LinkedHashSet<String>();

    /**
     * Create a WDG schematic reader.
     *
     * @param inputStream the compressed NBT input stream
     */
    public WdgSchematicReader(NBTInputStream inputStream) {
        if (inputStream == null) {
            throw new NullPointerException("inputStream");
        }
        this.inputStream = inputStream;
    }

    /**
     * Get the unique registry names that were unavailable during the most recent read.
     *
     * @return an immutable insertion-ordered set of missing registry names
     */
    public Set<String> getMissingBlockRegistryNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(missingBlockRegistryNames));
    }

    @Override
    public Clipboard read(WorldData data) throws IOException {
        missingBlockRegistryNames.clear();
        try {
            return readInternal(data);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("Malformed WDG schematic: " + safeMessage(e), e);
        }
    }

    private Clipboard readInternal(WorldData data) throws IOException {
        BlockRegistryNameResolver resolver = WdgSchematicFormat.requireResolver(data);
        NamedTag rootTag = inputStream.readNamedTag();
        if (!WdgSchematicFormat.ROOT_NAME.equals(rootTag.getName())) {
            throw new IOException("Expected root tag '" + WdgSchematicFormat.ROOT_NAME + "'");
        }
        if (!(rootTag.getTag() instanceof CompoundTag)) {
            throw new IOException("WDG schematic root tag is not a compound");
        }

        Map<String, Tag> schematic = ((CompoundTag) rootTag.getTag()).getValue();
        int version = requireTag(schematic, "Version", IntTag.class).getValue();
        if (version != WdgSchematicFormat.VERSION) {
            throw new IOException("Unsupported WDG schematic version " + version
                    + " (supported version: " + WdgSchematicFormat.VERSION + ")");
        }

        int width = requireTag(schematic, "Width", IntTag.class).getValue();
        int height = requireTag(schematic, "Height", IntTag.class).getValue();
        int length = requireTag(schematic, "Length", IntTag.class).getValue();
        int volume = WdgSchematicFormat.validateVolume(width, height, length);

        int minimumX = requireTag(schematic, "WEOriginX", IntTag.class).getValue();
        int minimumY = requireTag(schematic, "WEOriginY", IntTag.class).getValue();
        int minimumZ = requireTag(schematic, "WEOriginZ", IntTag.class).getValue();
        int offsetX = requireTag(schematic, "WEOffsetX", IntTag.class).getValue();
        int offsetY = requireTag(schematic, "WEOffsetY", IntTag.class).getValue();
        int offsetZ = requireTag(schematic, "WEOffsetZ", IntTag.class).getValue();

        WdgSchematicFormat.validateCoordinateRange(minimumX, width, "X");
        WdgSchematicFormat.validateCoordinateRange(minimumY, height, "Y");
        WdgSchematicFormat.validateCoordinateRange(minimumZ, length, "Z");
        WdgSchematicFormat.validateOriginCoordinate(minimumX, offsetX, "X");
        WdgSchematicFormat.validateOriginCoordinate(minimumY, offsetY, "Y");
        WdgSchematicFormat.validateOriginCoordinate(minimumZ, offsetZ, "Z");

        ListTag paletteTag = requireTag(schematic, "Palette", ListTag.class);
        if (paletteTag.getType() != StringTag.class) {
            throw new IOException("Palette list must contain string tags");
        }
        List<String> palette = readPalette(paletteTag);

        int[] blockIndexes = requireTag(schematic, "Blocks", IntArrayTag.class).getValue();
        byte[] metadata = requireTag(schematic, "Data", ByteArrayTag.class).getValue();
        if (blockIndexes.length != volume) {
            throw new IOException("Blocks length " + blockIndexes.length
                    + " does not match validated volume " + volume);
        }
        if (metadata.length != volume) {
            throw new IOException("Data length " + metadata.length
                    + " does not match validated volume " + volume);
        }

        boolean[] usedPalette = new boolean[palette.size()];
        for (int i = 0; i < volume; i++) {
            int paletteIndex = blockIndexes[i];
            if (paletteIndex < 0 || paletteIndex >= palette.size()) {
                throw new IOException("Blocks palette index is outside the palette at array index "
                        + i + ": " + paletteIndex);
            }
            usedPalette[paletteIndex] = true;

            int blockData = metadata[i] & 0xFF;
            if (blockData > BaseBlock.MAX_DATA) {
                throw new IOException("Block metadata is outside 0-" + BaseBlock.MAX_DATA
                        + " at array index " + i + ": " + blockData);
            }
        }

        Integer airId = null;
        int[] resolvedPalette = new int[palette.size()];
        boolean[] missingPalette = new boolean[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            if (!usedPalette[i]) {
                continue;
            }

            String registryName = palette.get(i);
            Integer blockId = resolver.getBlockId(registryName);
            if (blockId == null) {
                if (airId == null) {
                    airId = resolveAirId(resolver);
                }
                resolvedPalette[i] = airId;
                missingPalette[i] = true;
                missingBlockRegistryNames.add(registryName);
            } else {
                validateResolvedBlockId(blockId, registryName);
                resolvedPalette[i] = blockId;
            }
        }

        Map<BlockVector, CompoundTag> tileEntities = readTileEntities(
                requireTag(schematic, "TileEntities", ListTag.class), width, height, length);
        List<CompoundTag> entities = readEntities(requireTag(schematic, "Entities", ListTag.class));

        Vector minimum = new Vector(minimumX, minimumY, minimumZ);
        Vector maximum = minimum.add(width, height, length).subtract(Vector.ONE);
        Vector origin = minimum.subtract(offsetX, offsetY, offsetZ);
        Region region = new CuboidRegion(minimum, maximum);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(origin);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    int index = indexOf(x, y, z, width, length);
                    int paletteIndex = blockIndexes[index];
                    int blockData = missingPalette[paletteIndex] ? 0 : (metadata[index] & 0xFF);
                    BaseBlock block = new BaseBlock(resolvedPalette[paletteIndex], blockData);
                    BlockVector relative = new BlockVector(x, y, z);
                    if (!missingPalette[paletteIndex]) {
                        CompoundTag tileEntity = tileEntities.get(relative);
                        if (tileEntity != null) {
                            block.setNbtData(tileEntity);
                        }
                    }

                    try {
                        if (!clipboard.setBlock(minimum.add(relative), block)) {
                            throw new IOException("Failed to set clipboard block at " + x + "," + y + "," + z);
                        }
                    } catch (WorldEditException e) {
                        throw new IOException("Failed to set clipboard block at " + x + "," + y + "," + z, e);
                    }
                }
            }
        }

        for (CompoundTag entityTag : entities) {
            String typeId = ((StringTag) entityTag.getValue().get("id")).getValue();
            ListTag positionTag = (ListTag) entityTag.getValue().get("Pos");
            ListTag rotationTag = (ListTag) entityTag.getValue().get("Rotation");
            Vector position = new Vector(
                    ((DoubleTag) positionTag.getValue().get(0)).getValue(),
                    ((DoubleTag) positionTag.getValue().get(1)).getValue(),
                    ((DoubleTag) positionTag.getValue().get(2)).getValue()).add(minimum);
            float yaw = ((FloatTag) rotationTag.getValue().get(0)).getValue();
            float pitch = ((FloatTag) rotationTag.getValue().get(1)).getValue();
            clipboard.createEntity(new Location(clipboard, position, yaw, pitch), new BaseEntity(typeId, entityTag));
        }

        return clipboard;
    }

    private List<String> readPalette(ListTag paletteTag) throws IOException {
        List<String> palette = new ArrayList<String>();
        Set<String> uniqueNames = new LinkedHashSet<String>();
        for (int i = 0; i < paletteTag.getValue().size(); i++) {
            Tag tag = paletteTag.getValue().get(i);
            if (!(tag instanceof StringTag)) {
                throw new IOException("Palette entry " + i + " is not a string tag");
            }
            String registryName = ((StringTag) tag).getValue();
            if (!WdgSchematicFormat.isValidRegistryName(registryName)) {
                throw new IOException("Palette entry " + i + " is not a complete registry name: "
                        + registryName);
            }
            if (!uniqueNames.add(registryName)) {
                throw new IOException("Palette contains duplicate registry name: " + registryName);
            }
            palette.add(registryName);
        }
        if (palette.isEmpty()) {
            throw new IOException("Palette must contain at least one registry name");
        }
        return palette;
    }

    private int resolveAirId(BlockRegistryNameResolver resolver) throws IOException {
        Integer airId = resolver.getBlockId(WdgSchematicFormat.AIR_REGISTRY_NAME);
        if (airId == null) {
            throw new IOException("The active registry does not contain "
                    + WdgSchematicFormat.AIR_REGISTRY_NAME + " for missing-block replacement");
        }
        validateResolvedBlockId(airId, WdgSchematicFormat.AIR_REGISTRY_NAME);
        return airId;
    }

    private void validateResolvedBlockId(int blockId, String registryName) throws IOException {
        if (blockId < 0 || blockId > BaseBlock.MAX_ID) {
            throw new IOException("Registry name " + registryName + " resolved to unsupported block ID "
                    + blockId + " (expected 0-" + BaseBlock.MAX_ID + ")");
        }
    }

    private Map<BlockVector, CompoundTag> readTileEntities(ListTag listTag, int width,
                                                            int height, int length) throws IOException {
        if (listTag.getType() != CompoundTag.class) {
            throw new IOException("TileEntities list must contain compound tags");
        }

        Map<BlockVector, CompoundTag> result = new HashMap<BlockVector, CompoundTag>();
        for (int i = 0; i < listTag.getValue().size(); i++) {
            Tag tag = listTag.getValue().get(i);
            if (!(tag instanceof CompoundTag)) {
                throw new IOException("TileEntities entry " + i + " is not a compound tag");
            }
            CompoundTag compound = (CompoundTag) tag;
            Map<String, Tag> values = compound.getValue();
            int x = requireTag(values, "x", IntTag.class).getValue();
            int y = requireTag(values, "y", IntTag.class).getValue();
            int z = requireTag(values, "z", IntTag.class).getValue();
            Tag idTag = values.get("id");
            if (!(idTag instanceof StringTag) || ((StringTag) idTag).getValue().isEmpty()) {
                throw new IOException("TileEntities entry " + i + " is missing a non-empty string id");
            }
            if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= length) {
                throw new IOException("TileEntities entry " + i + " has coordinates outside the region: "
                        + x + "," + y + "," + z);
            }
            BlockVector position = new BlockVector(x, y, z);
            if (result.containsKey(position)) {
                throw new IOException("Duplicate tile entity coordinates at " + x + "," + y + "," + z);
            }
            result.put(position, compound);
        }
        return result;
    }

    private List<CompoundTag> readEntities(ListTag listTag) throws IOException {
        if (listTag.getType() != CompoundTag.class) {
            throw new IOException("Entities list must contain compound tags");
        }

        List<CompoundTag> result = new ArrayList<CompoundTag>();
        for (int i = 0; i < listTag.getValue().size(); i++) {
            Tag tag = listTag.getValue().get(i);
            if (!(tag instanceof CompoundTag)) {
                throw new IOException("Entities entry " + i + " is not a compound tag");
            }
            CompoundTag compound = (CompoundTag) tag;
            Map<String, Tag> values = compound.getValue();
            String typeId = requireTag(values, "id", StringTag.class).getValue();
            if (typeId.isEmpty()) {
                throw new IOException("Entities entry " + i + " has an empty id");
            }
            validateEntityPosition(requireTag(values, "Pos", ListTag.class), i);
            validateEntityRotation(requireTag(values, "Rotation", ListTag.class), i);
            result.add(compound);
        }
        return result;
    }

    private void validateEntityPosition(ListTag position, int entityIndex) throws IOException {
        if (position.getType() != DoubleTag.class || position.getValue().size() != 3) {
            throw new IOException("Entities entry " + entityIndex
                    + " must have a Pos list containing exactly three doubles");
        }
        for (int i = 0; i < 3; i++) {
            Tag tag = position.getValue().get(i);
            if (!(tag instanceof DoubleTag) || !isFinite(((DoubleTag) tag).getValue())) {
                throw new IOException("Entities entry " + entityIndex + " has invalid Pos value " + i);
            }
        }
    }

    private void validateEntityRotation(ListTag rotation, int entityIndex) throws IOException {
        if (rotation.getType() != FloatTag.class || rotation.getValue().size() != 2) {
            throw new IOException("Entities entry " + entityIndex
                    + " must have a Rotation list containing exactly two floats");
        }
        for (int i = 0; i < 2; i++) {
            Tag tag = rotation.getValue().get(i);
            if (!(tag instanceof FloatTag) || !isFinite(((FloatTag) tag).getValue())) {
                throw new IOException("Entities entry " + entityIndex + " has invalid Rotation value " + i);
            }
        }
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static int indexOf(int x, int y, int z, int width, int length) {
        return y * width * length + z * width + x;
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isEmpty() ? exception.getClass().getSimpleName() : message;
    }

    private static <T extends Tag> T requireTag(Map<String, Tag> tags, String key,
                                                 Class<T> expected) throws IOException {
        Tag tag = tags.get(key);
        if (tag == null) {
            throw new IOException("WDG schematic is missing a '" + key + "' tag");
        }
        if (!expected.isInstance(tag)) {
            throw new IOException("WDG schematic tag '" + key + "' is not " + expected.getSimpleName());
        }
        return expected.cast(tag);
    }
}
