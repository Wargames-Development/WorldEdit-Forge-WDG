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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ForgePasteRoutingTest {
    @Test
    public void exposesSlashlessPasteAndForwardsEveryAliasToSameCommand() throws Exception {
        WorldEdit worldEdit = mock(WorldEdit.class);
        ParametricBuilder builder = new ParametricBuilder();
        builder.addBinding(new WorldEditBinding(worldEdit));
        SimpleDispatcher dispatcher = new SimpleDispatcher();
        builder.registerMethodsAsCommands(dispatcher, new ClipboardCommands(worldEdit));
        CommandMapping paste = dispatcher.get("paste");
        assertSame(paste, dispatcher.get("/paste"));
        assertSame(paste, dispatcher.get("wdgpaste"));
        assertTrue(paste.getDescription().getPermissions().contains("worldedit.clipboard.paste"));
        CommandWrapper wrapper = new CommandWrapper(paste);
        assertEquals("paste", wrapper.getCommandName());
        assertTrue(wrapper.getCommandAliases().containsAll(Arrays.asList("paste", "/paste", "wdgpaste")));
        assertSame(paste, dispatcher.get(wrapper.getCommandName()));
    }
}
