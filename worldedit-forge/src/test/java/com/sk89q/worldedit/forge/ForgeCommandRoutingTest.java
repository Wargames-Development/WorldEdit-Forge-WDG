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

import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Dispatcher;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ForgeCommandRoutingTest {
    private Dispatcher commands() {
        return WorldEdit.getInstance().getPlatformManager().getCommandManager().getDispatcher();
    }

    @Test
    public void everySlashCommandSurvivesTheBukkitHandoffWithoutPlainAliases() {
        Dispatcher dispatcher = commands();
        ICommandManager manager = mock(ICommandManager.class);
        ICommandSender sender = mock(ICommandSender.class);
        Map<String, ICommand> nativeCommands = new HashMap<String, ICommand>();
        for (CommandMapping mapping : dispatcher.getCommands()) {
            CommandWrapper command = new CommandWrapper(mapping);
            for (String alias : mapping.getAllAliases()) nativeCommands.put(alias, command);
        }
        int checked = 0;
        for (String alias : dispatcher.getAliases()) {
            if (!alias.startsWith("/")) continue;
            CommandWrapper command = (CommandWrapper) nativeCommands.get(alias);
            CrucibleCommandBridge.NativeCommandBridge bridge =
                    new CrucibleCommandBridge.NativeCommandBridge(manager, command);
            String[] arguments = {"-a", "1,2,3", "minecraft:stone"};
            bridge.processCommand(sender, arguments);
            String input = "/" + command.getCommandName() + " -a 1,2,3 minecraft:stone";
            verify(manager).executeCommand(sender, input);
            reset(manager);
            // The actual native parser removes one slash before looking up its key.
            assertSame(command, nativeCommands.get(input.substring(1).split(" ")[0]));
            assertEquals(command.getCommandName(), bridge.getCommandName());
            assertEquals(Arrays.asList(dispatcher.get(alias).getAllAliases()), bridge.getCommandAliases());
            checked++;
        }
        assertTrue("The complete slash-command catalog must be covered", checked > 50);
        assertTrue(nativeCommands.containsKey("/")); // bare // superpickaxe toggle
        assertFalse(nativeCommands.containsKey("copy"));
        assertFalse(nativeCommands.containsKey("set"));
    }

    @Test
    public void bridgePreservesNoArgumentsAndTabCompletion() {
        ICommandManager manager = mock(ICommandManager.class);
        CommandWrapper command = mock(CommandWrapper.class);
        ICommandSender sender = mock(ICommandSender.class);
        when(command.getCommandName()).thenReturn("/copy");
        when(command.canCommandSenderUseCommand(sender)).thenReturn(true);
        when(command.addTabCompletionOptions(sender, new String[]{"-"})).thenReturn(Arrays.asList("-e"));
        CrucibleCommandBridge.NativeCommandBridge bridge =
                new CrucibleCommandBridge.NativeCommandBridge(manager, command);
        bridge.processCommand(sender, new String[0]);
        verify(manager).executeCommand(sender, "//copy");
        assertTrue(bridge.canCommandSenderUseCommand(sender));
        assertEquals(Arrays.asList("-e"), bridge.addTabCompletionOptions(sender, new String[]{"-"}));
    }

    @Test
    public void ordinaryForgeDoesNotNeedBukkit() {
        org.junit.Assume.assumeTrue(!Boolean.getBoolean("worldedit.crucibleFixture"));
        ICommandManager manager = mock(ICommandManager.class);
        CrucibleCommandBridge.install(manager);
        verifyZeroInteractions(manager);
    }

    @Test
    public void unrelatedNativeCommandsAreNeverForwardedToWorldEdit() {
        assertNull(CommandWrapper.getWorldEditCommandLine(mock(ICommand.class), new String[]{"copy"}));
    }

    @Test
    public void forwardingStillEnforcesWorldEditPermissions() throws Exception {
        Dispatcher dispatcher = commands();
        CommandWrapper command = new CommandWrapper(dispatcher.get("/copy"));
        CommandLocals locals = new CommandLocals();
        locals.put(Actor.class, mock(Player.class));
        try {
            dispatcher.call(CommandWrapper.getWorldEditCommandLine(command, new String[0]), locals, new String[0]);
            fail("Forwarded copy must still require worldedit.clipboard.copy");
        } catch (CommandPermissionsException expected) {
            // The ordinary WorldEdit dispatcher checks permissions before editing.
        }
    }
}
