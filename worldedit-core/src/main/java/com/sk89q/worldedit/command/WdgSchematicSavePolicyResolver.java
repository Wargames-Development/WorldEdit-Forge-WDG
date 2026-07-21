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

package com.sk89q.worldedit.command;

import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.WdgTileEntityPolicy;

import javax.annotation.Nullable;

/**
 * Resolves the effective per-save WDG tile-entity policy.
 */
final class WdgSchematicSavePolicyResolver {

    private WdgSchematicSavePolicyResolver() {
    }

    @Nullable
    static WdgTileEntityPolicy resolve(ClipboardFormat format,
                                       @Nullable WdgTileEntityPolicy configuredPolicy,
                                       boolean preserveSwitch, boolean stripSwitch)
            throws CommandException {
        if (preserveSwitch && stripSwitch) {
            throw new CommandException("-p and -s cannot be used together.");
        }

        if ((preserveSwitch || stripSwitch) && format != ClipboardFormat.WDG_SCHEMATIC) {
            throw new CommandException("-p and -s are supported only for WDG saves.");
        }

        if (format != ClipboardFormat.WDG_SCHEMATIC) {
            return null;
        }
        if (preserveSwitch) {
            return WdgTileEntityPolicy.PRESERVE;
        }
        if (stripSwitch) {
            return WdgTileEntityPolicy.STRIP;
        }
        return configuredPolicy == null ? WdgTileEntityPolicy.PRESERVE : configuredPolicy;
    }
}
