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

package com.sk89q.worldedit.world.registry;

import javax.annotation.Nullable;

/**
 * Resolves block identity between legacy numeric IDs and stable platform
 * registry names.
 *
 * <p>This is an optional capability. Callers should check whether the active
 * {@link BlockRegistry} implements this interface before using it.</p>
 */
public interface BlockRegistryNameResolver {

    /**
     * Get the stable registry name for a currently registered block ID.
     *
     * @param blockId the current numeric block ID
     * @return the complete registry name, or null if the ID is not registered
     */
    @Nullable
    String getRegistryName(int blockId);

    /**
     * Get the current numeric block ID for an exact stable registry name.
     *
     * @param registryName the complete registry name
     * @return the current numeric block ID, or null if the name is invalid or absent
     */
    @Nullable
    Integer getBlockId(String registryName);

}
