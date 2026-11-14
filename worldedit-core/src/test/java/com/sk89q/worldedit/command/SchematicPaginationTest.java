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
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.auth.Authorizer;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SchematicPaginationTest {
    @Test
    public void parsesPageArgumentsThroughRealCommandDispatcher() throws Exception {
        File directory = Files.createTempDirectory("schematics").toFile();
        try {
            for (int i = 0; i < 18; i++) {
                assertTrue(new File(directory, String.format("%02d.schematic", i)).createNewFile());
            }
            WorldEdit worldEdit = mock(WorldEdit.class);
            LocalConfiguration config = mock(LocalConfiguration.class);
            config.saveDir = directory.getName();
            when(worldEdit.getConfiguration()).thenReturn(config);
            when(worldEdit.getWorkingDirectoryFile(config.saveDir)).thenReturn(directory);
            Actor actor = mock(Actor.class);
            CommandLocals locals = new CommandLocals();
            locals.put(Actor.class, actor);
            ParametricBuilder builder = new ParametricBuilder();
            builder.setAuthorizer(new Authorizer() {
                @Override
                public boolean testPermission(CommandLocals context, String permission) {
                    return true;
                }
            });
            builder.addBinding(new WorldEditBinding(worldEdit));
            SimpleDispatcher dispatcher = new SimpleDispatcher();
            builder.registerMethodsAsCommands(dispatcher, new SchematicCommands(worldEdit));
            for (String command : new String[] {"list 2", "list -p 2", "list 1 -p 2", "list -n 2", "ls 2", "all 2"}) {
                reset(actor);
                dispatcher.call(command, locals, new String[0]);
                verify(actor).print("Available schematics (Filename: Format) [2/2]:");
            }
            reset(actor);
            dispatcher.call("list", locals, new String[0]);
            verify(actor).print("Available schematics (Filename: Format) [1/2]:");
            reset(actor);
            dispatcher.call("list 0", locals, new String[0]);
            verify(actor).printError("Page must be at least 1");
            reset(actor);
            dispatcher.call("list 3", locals, new String[0]);
            verify(actor).printError("Page must be less than 3");
            try {
                dispatcher.call("list nonsense", locals, new String[0]);
                fail("Invalid page should fail parsing");
            } catch (CommandException expected) {
                // The command parser supplies an integer validation error.
            }
        } finally {
            for (File file : directory.listFiles()) {
                file.delete();
            }
            directory.delete();
        }
    }
}
