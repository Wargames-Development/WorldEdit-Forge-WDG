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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests bounded command policy resolution without a command framework fixture.
 */
public class WdgSchematicSavePolicyResolverTest {

    @Test
    public void testConfiguredDefaultsAndExplicitOverrides() throws Exception {
        assertEquals(WdgTileEntityPolicy.PRESERVE, resolve(
                ClipboardFormat.WDG_SCHEMATIC, WdgTileEntityPolicy.PRESERVE, false, false));
        assertEquals(WdgTileEntityPolicy.STRIP, resolve(
                ClipboardFormat.WDG_SCHEMATIC, WdgTileEntityPolicy.STRIP, false, false));
        assertEquals(WdgTileEntityPolicy.PRESERVE, resolve(
                ClipboardFormat.WDG_SCHEMATIC, WdgTileEntityPolicy.STRIP, true, false));
        assertEquals(WdgTileEntityPolicy.STRIP, resolve(
                ClipboardFormat.WDG_SCHEMATIC, WdgTileEntityPolicy.PRESERVE, false, true));
        assertEquals(WdgTileEntityPolicy.PRESERVE, resolve(
                ClipboardFormat.WDG_SCHEMATIC, null, false, false));
    }

    @Test
    public void testConflictingSwitchesFail() throws Exception {
        expectFailure("cannot be used together", ClipboardFormat.WDG_SCHEMATIC,
                WdgTileEntityPolicy.PRESERVE, true, true);
    }

    @Test
    public void testLegacySwitchesFailAndNoSwitchStaysLegacy() throws Exception {
        expectFailure("only for WDG saves", ClipboardFormat.SCHEMATIC,
                WdgTileEntityPolicy.PRESERVE, true, false);
        expectFailure("only for WDG saves", ClipboardFormat.SCHEMATIC,
                WdgTileEntityPolicy.STRIP, false, true);
        assertNull(resolve(ClipboardFormat.SCHEMATIC,
                WdgTileEntityPolicy.STRIP, false, false));
    }

    private WdgTileEntityPolicy resolve(ClipboardFormat format, WdgTileEntityPolicy configured,
                                        boolean preserve, boolean strip) throws CommandException {
        return WdgSchematicSavePolicyResolver.resolve(format, configured, preserve, strip);
    }

    private void expectFailure(String message, ClipboardFormat format,
                               WdgTileEntityPolicy configured, boolean preserve, boolean strip)
            throws Exception {
        try {
            resolve(format, configured, preserve, strip);
            fail("Expected CommandException containing: " + message);
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains(message));
        }
    }
}
