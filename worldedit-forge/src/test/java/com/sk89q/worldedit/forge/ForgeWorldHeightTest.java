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

import net.minecraft.world.World;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForgeWorldHeightTest {
    @Test
    public void usesActiveWorldHeightWithoutGlobalHeightCache() {
        World world = mock(World.class);
        ForgeWorld adapter = new ForgeWorld(world);
        for (int height : new int[] {256, 384, 400, 512, 1024}) {
            when(world.getHeight()).thenReturn(height);
            assertEquals(height - 1, adapter.getMaxY());
            assertEquals(height - 1, adapter.getMaximumPoint().getBlockY());
            assertEquals(0, adapter.getMinimumPoint().getBlockY());
        }
        World otherWorld = mock(World.class);
        when(otherWorld.getHeight()).thenReturn(256);
        assertEquals(255, new ForgeWorld(otherWorld).getMaxY());
        assertEquals(1023, adapter.getMaxY());
    }
}
