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

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Controls whether a WDG schematic save preserves complete tile-entity NBT.
 */
public enum WdgTileEntityPolicy {

    PRESERVE("preserve"),
    STRIP("strip");

    private final String serializedValue;

    WdgTileEntityPolicy(String serializedValue) {
        this.serializedValue = serializedValue;
    }

    /**
     * Get the canonical configuration and file value.
     *
     * @return the canonical lower-case value
     */
    public String getSerializedValue() {
        return serializedValue;
    }

    /**
     * Parse a configuration value using trimmed, case-insensitive matching.
     *
     * @param value the configured value
     * @return the matched policy, or null when unsupported
     */
    @Nullable
    public static WdgTileEntityPolicy fromConfigurationValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        for (WdgTileEntityPolicy policy : values()) {
            if (policy.serializedValue.equals(normalized)) {
                return policy;
            }
        }
        return null;
    }

    /**
     * Parse a canonical value recorded in a WDG schematic file.
     *
     * @param value the recorded file value
     * @return the matched policy, or null when unsupported
     */
    @Nullable
    public static WdgTileEntityPolicy fromSerializedValue(String value) {
        if (value == null) {
            return null;
        }

        for (WdgTileEntityPolicy policy : values()) {
            if (policy.serializedValue.equals(value)) {
                return policy;
            }
        }
        return null;
    }
}
