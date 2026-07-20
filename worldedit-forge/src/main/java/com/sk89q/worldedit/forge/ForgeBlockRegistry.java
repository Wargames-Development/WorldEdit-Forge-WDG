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
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.LegacyBlockRegistry;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;

import javax.annotation.Nullable;

/**
 * Forge-aware block registry that preserves legacy parsing while exposing
 * strict registry-name identity resolution.
 */
class ForgeBlockRegistry extends LegacyBlockRegistry implements BlockRegistryNameResolver {

    private final RegistryAccess registry;

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

    }

}
