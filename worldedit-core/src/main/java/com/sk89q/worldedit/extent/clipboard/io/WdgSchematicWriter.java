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
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.WorldData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes WDG registry-stable schematic version 1 files.
 */
public class WdgSchematicWriter implements ClipboardWriter {

    private final NBTOutputStream outputStream;

    /**
     * Create a WDG schematic writer.
     *
     * @param outputStream the compressed NBT output stream
     */
    public WdgSchematicWriter(NBTOutputStream outputStream) {
        if (outputStream == null) {
            throw new NullPointerException("outputStream");
        }
        this.outputStream = outputStream;
    }

    @Override
    public void write(Clipboard clipboard, WorldData data) throws IOException {
        if (clipboard == null) {
            throw new NullPointerException("clipboard");
        }

        BlockRegistryNameResolver resolver = WdgSchematicFormat.requireResolver(data);
        Region region = clipboard.getRegion();
        Vector origin = clipboard.getOrigin();
        Vector minimum = region.getMinimumPoint();
        Vector offset = minimum.subtract(origin);
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();
        int volume = WdgSchematicFormat.validateVolume(width, height, length);

        int[] blocks = new int[volume];
        byte[] metadata = new byte[volume];
        List<Tag> tileEntities = new ArrayList<Tag>();
        LinkedHashMap<String, Integer> paletteIndexes = new LinkedHashMap<String, Integer>();

        for (Vector point : region) {
            Vector relative = point.subtract(minimum);
            int x = relative.getBlockX();
            int y = relative.getBlockY();
            int z = relative.getBlockZ();
            int index = indexOf(x, y, z, width, length);
            BaseBlock block = clipboard.getBlock(point);

            String registryName = resolver.getRegistryName(block.getId());
            if (!WdgSchematicFormat.isValidRegistryName(registryName)) {
                throw new IOException("Cannot save WDG schematic: block ID " + block.getId()
                        + " has no exact active registry name");
            }

            Integer exactId = resolver.getBlockId(registryName);
            if (exactId == null || exactId.intValue() != block.getId()) {
                throw new IOException("Cannot save WDG schematic: registry name " + registryName
                        + " does not resolve exactly to block ID " + block.getId());
            }

            Integer paletteIndex = paletteIndexes.get(registryName);
            if (paletteIndex == null) {
                paletteIndex = paletteIndexes.size();
                paletteIndexes.put(registryName, paletteIndex);
            }
            blocks[index] = paletteIndex;

            int blockData = block.getData();
            if (blockData < 0 || blockData > BaseBlock.MAX_DATA) {
                throw new IOException("Cannot save WDG schematic: block metadata is outside 0-"
                        + BaseBlock.MAX_DATA + " at " + x + "," + y + "," + z + ": " + blockData);
            }
            metadata[index] = (byte) blockData;

            CompoundTag rawTileEntity = block.getNbtData();
            if (rawTileEntity != null) {
                Tag idTag = rawTileEntity.getValue().get("id");
                if (!(idTag instanceof StringTag) || ((StringTag) idTag).getValue().isEmpty()) {
                    throw new IOException("Cannot save WDG schematic: tile entity at " + x + "," + y
                            + "," + z + " is missing a non-empty string id");
                }

                Map<String, Tag> values = new LinkedHashMap<String, Tag>(rawTileEntity.getValue());
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
                tileEntities.add(new CompoundTag(values));
            }
        }

        List<Tag> palette = new ArrayList<Tag>(paletteIndexes.size());
        for (String registryName : paletteIndexes.keySet()) {
            palette.add(new StringTag(registryName));
        }

        Map<String, Tag> schematic = new LinkedHashMap<String, Tag>();
        schematic.put("Version", new IntTag(WdgSchematicFormat.VERSION));
        schematic.put("Width", new IntTag(width));
        schematic.put("Height", new IntTag(height));
        schematic.put("Length", new IntTag(length));
        schematic.put("WEOriginX", new IntTag(minimum.getBlockX()));
        schematic.put("WEOriginY", new IntTag(minimum.getBlockY()));
        schematic.put("WEOriginZ", new IntTag(minimum.getBlockZ()));
        schematic.put("WEOffsetX", new IntTag(offset.getBlockX()));
        schematic.put("WEOffsetY", new IntTag(offset.getBlockY()));
        schematic.put("WEOffsetZ", new IntTag(offset.getBlockZ()));
        schematic.put("Palette", new ListTag(StringTag.class, palette));
        schematic.put("Blocks", new IntArrayTag(blocks));
        schematic.put("Data", new ByteArrayTag(metadata));
        schematic.put("TileEntities", new ListTag(CompoundTag.class, tileEntities));
        schematic.put("Entities", writeEntities(clipboard, minimum));

        outputStream.writeNamedTag(WdgSchematicFormat.ROOT_NAME, new CompoundTag(schematic));
    }

    private ListTag writeEntities(Clipboard clipboard, Vector minimum) throws IOException {
        List<Tag> entities = new ArrayList<Tag>();
        for (Entity entity : clipboard.getEntities()) {
            BaseEntity state = entity.getState();
            if (state == null) {
                continue;
            }

            String typeId = state.getTypeId();
            if (typeId == null || typeId.isEmpty()) {
                throw new IOException("Cannot save WDG schematic: an entity has no type id");
            }

            Map<String, Tag> values = new LinkedHashMap<String, Tag>();
            CompoundTag rawTag = state.getNbtData();
            if (rawTag != null) {
                values.putAll(rawTag.getValue());
            }

            Location location = entity.getLocation();
            Vector relativePosition = location.toVector().subtract(minimum);
            if (!isFinite(relativePosition.getX()) || !isFinite(relativePosition.getY())
                    || !isFinite(relativePosition.getZ()) || !isFinite(location.getYaw())
                    || !isFinite(location.getPitch())) {
                throw new IOException("Cannot save WDG schematic: an entity has a non-finite "
                        + "position or rotation");
            }
            values.put("id", new StringTag(typeId));
            values.put("Pos", writeVector(relativePosition));
            values.put("Rotation", writeRotation(location));
            entities.add(new CompoundTag(values));
        }

        return new ListTag(CompoundTag.class, entities);
    }

    private ListTag writeVector(Vector vector) {
        List<Tag> list = new ArrayList<Tag>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private ListTag writeRotation(Location location) {
        List<Tag> list = new ArrayList<Tag>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static int indexOf(int x, int y, int z, int width, int length) {
        return y * width * length + z * width + x;
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
