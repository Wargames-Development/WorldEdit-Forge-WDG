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

import net.minecraft.block.Block;
import net.minecraft.world.chunk.Chunk;

/**
 * Places a block while preserving the exact metadata requested by WorldEdit.
 */
final class ForgeBlockPlacement {

    private ForgeBlockPlacement() {
    }

    static boolean setBlockAndRestoreMetadata(Chunk chunk, int x, int y, int z, Block block, int data) {
        boolean successful = chunk.func_150807_a(x, y, z, block, data);
        if (successful && chunk.getBlockMetadata(x, y, z) != data) {
            // onBlockAdded() may choose a placement-facing value for blocks such as chests and furnaces.
            // WorldEdit is restoring an existing clipboard state, so reapply the exact stored metadata.
            chunk.setBlockMetadata(x, y, z, data);
        }
        return successful;
    }

}
