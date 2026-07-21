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

import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.WorldData;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Constants and validation shared by WDG schematic version 1 readers and writers.
 *
 * <p>The compressed NBT root is {@code WDGSchematic}. Version 1 stores integer
 * dimensions and clipboard origin/offset tags, a {@code List<String>} palette,
 * an integer palette-index array, a separate metadata byte array, and compound
 * lists for tile entities and entities.</p>
 */
final class WdgSchematicFormat {

    static final String ROOT_NAME = "WDGSchematic";
    static final int VERSION = 1;
    static final String TILE_ENTITY_POLICY_TAG = "TileEntityPolicy";
    static final String AIR_REGISTRY_NAME = "minecraft:air";

    private static final Pattern REGISTRY_NAME =
            Pattern.compile("^[A-Za-z0-9_.-]+:[A-Za-z0-9_./-]+$");

    private WdgSchematicFormat() {
    }

    static int validateVolume(int width, int height, int length) throws IOException {
        if (width <= 0) {
            throw new IOException("WDG schematic width must be greater than zero: " + width);
        }
        if (height <= 0) {
            throw new IOException("WDG schematic height must be greater than zero: " + height);
        }
        if (length <= 0) {
            throw new IOException("WDG schematic length must be greater than zero: " + length);
        }

        long widthHeight = (long) width * (long) height;
        if (widthHeight > Integer.MAX_VALUE) {
            throw new IOException("WDG schematic volume is too large for Java arrays");
        }

        long volume = widthHeight * (long) length;
        if (volume > Integer.MAX_VALUE) {
            throw new IOException("WDG schematic volume is too large for Java arrays: " + volume);
        }

        return (int) volume;
    }

    static void validateCoordinateRange(int minimum, int size, String axis) throws IOException {
        long maximum = (long) minimum + (long) size - 1L;
        if (maximum < Integer.MIN_VALUE || maximum > Integer.MAX_VALUE) {
            throw new IOException("WDG schematic " + axis + " coordinate range overflows integers");
        }
    }

    static void validateOriginCoordinate(int minimum, int offset, String axis) throws IOException {
        long origin = (long) minimum - (long) offset;
        if (origin < Integer.MIN_VALUE || origin > Integer.MAX_VALUE) {
            throw new IOException("WDG schematic " + axis + " origin overflows integers");
        }
    }

    static boolean isValidRegistryName(String registryName) {
        return registryName != null && REGISTRY_NAME.matcher(registryName).matches();
    }

    static BlockRegistryNameResolver requireResolver(WorldData data) throws IOException {
        if (data == null) {
            throw new IOException("The active world does not provide registry data for WDG schematics");
        }

        BlockRegistry blockRegistry = data.getBlockRegistry();
        if (!(blockRegistry instanceof BlockRegistryNameResolver)) {
            throw new IOException("The active platform does not support registry-stable WDG schematics");
        }

        return (BlockRegistryNameResolver) blockRegistry;
    }
}
