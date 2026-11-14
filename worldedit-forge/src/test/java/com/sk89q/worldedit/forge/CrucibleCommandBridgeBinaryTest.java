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
import com.sk89q.worldedit.util.command.CommandMapping;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

/** Exercises the actual Bukkit classes from -PcrucibleTestJar, without starting a server. */
public class CrucibleCommandBridgeBinaryTest {
    @Test
    @SuppressWarnings("unchecked")
    public void replacesOnlyWorldEditWrappersAndDispatchesEverySlashAlias() throws Exception {
        assumeTrue(Boolean.getBoolean("worldedit.crucibleFixture"));
        String craft = "org.bukkit.craftbukkit.v1_7_R4";
        Class<?> commandType = Class.forName("org.bukkit.command.Command");
        Class<?> mapType = Class.forName(craft + ".command.CraftSimpleCommandMap");
        Class<?> modType = Class.forName(craft + ".command.ModCustomCommand");
        Object map = mock(mapType);
        // Mockito bypasses constructors. Set up the actual inherited registry so the
        // real bridge can replace it without bootstrapping Bukkit's default commands.
        Map<String, Object> bukkitCommands = new HashMap<String, Object>();
        Field known = mapType.getSuperclass().getDeclaredField("knownCommands");
        known.setAccessible(true);
        known.set(map, bukkitCommands);
        Map<String, ICommand> nativeCommands = new HashMap<String, ICommand>();
        ICommandManager manager = mock(ICommandManager.class);
        when(manager.getCommands()).thenReturn(nativeCommands);
        for (CommandMapping mapping : WorldEdit.getInstance().getPlatformManager()
                .getCommandManager().getDispatcher().getCommands()) {
            CommandWrapper wrapper = new CommandWrapper(mapping);
            Object original = modType.getConstructor(String.class).newInstance(wrapper.getCommandName());
            commandType.getMethod("setPermission", String.class).invoke(original, CommandWrapper.class.getName());
            for (String alias : mapping.getAllAliases()) {
                nativeCommands.put(alias, wrapper);
                bukkitCommands.put(alias, original);
                bukkitCommands.put("worldedit:" + alias, original);
            }
        }
        Object unrelated = modType.getConstructor(String.class).newInstance("othermod");
        bukkitCommands.put("othermod", unrelated);
        nativeCommands.put("othermod", mock(ICommand.class));
        Object plugin = modType.getConstructor(String.class).newInstance("plugin-copy");
        bukkitCommands.put("copy", plugin);
        Object collidingMod = modType.getConstructor(String.class).newInstance("/copy");
        commandType.getMethod("setPermission", String.class).invoke(collidingMod, "other.ModCommand");
        bukkitCommands.put("othermod:/copy", collidingMod);
        Map<String, Object> before = new HashMap<String, Object>(bukkitCommands);
        int count = CrucibleCommandBridge.install(manager, map, craft);
        assertTrue(count > 40);
        assertEquals(before.keySet(), bukkitCommands.keySet());
        assertSame(unrelated, bukkitCommands.get("othermod"));
        assertSame(plugin, bukkitCommands.get("copy"));
        assertSame(collidingMod, bukkitCommands.get("othermod:/copy"));
        assertEquals(0, CrucibleCommandBridge.install(manager, map, craft));

        Class<?> bukkitSenderType = Class.forName(craft + ".command.CraftBlockCommandSender");
        Object bukkitSender = mock(bukkitSenderType);
        when(bukkitSenderType.getMethod("hasPermission", String.class)
                .invoke(bukkitSender, CommandWrapper.class.getName())).thenReturn(true);
        CommandBlockLogic sender = mock(CommandBlockLogic.class);
        when(bukkitSenderType.getMethod("getTileEntity").invoke(bukkitSender)).thenReturn(sender);
        when(sender.getEntityWorld()).thenReturn(mock(WorldServer.class));
        MinecraftServer server = mock(MinecraftServer.class);
        Field singleton = MinecraftServer.class.getDeclaredField("mcServer");
        singleton.setAccessible(true);
        Object previousServer = singleton.get(null);
        WorldServer[] worlds = new WorldServer[0];
        server.worldServers = worlds;
        singleton.set(null, server);
        try {
            Class<?> senderType = Class.forName("org.bukkit.command.CommandSender");
            Method dispatch = mapType.getMethod("dispatch", senderType, String.class);
            when(dispatch.invoke(map, any(senderType), anyString())).thenCallRealMethod();
            when(mapType.getMethod("getCommand", String.class).invoke(map, anyString())).thenCallRealMethod();
            for (String alias : nativeCommands.keySet()) {
                if (!alias.startsWith("/")) continue;
                Object replacement = bukkitCommands.get(alias);
                assertEquals(craft + ".command.VanillaCommandWrapper", replacement.getClass().getName());
                assertEquals(CommandWrapper.class.getName(), commandType.getMethod("getPermission").invoke(replacement));
                assertSame(replacement, bukkitCommands.get("worldedit:" + alias));
                assertEquals(true, dispatch.invoke(map, bukkitSender, alias + " -a 1,2,3"));
                assertEquals(true, dispatch.invoke(map, bukkitSender, "worldedit:" + alias + " -a 1,2,3"));
                verify(manager, times(2)).executeCommand(sender,
                        "/" + nativeCommands.get(alias).getCommandName() + " -a 1,2,3");
                reset(manager);
                when(manager.getCommands()).thenReturn(nativeCommands);
                assertSame("Bukkit must restore the server world list", worlds, server.worldServers);
            }
            when(bukkitSenderType.getMethod("hasPermission", String.class)
                    .invoke(bukkitSender, CommandWrapper.class.getName())).thenReturn(false);
            assertEquals(true, dispatch.invoke(map, bukkitSender, "/copy"));
            verify(manager, never()).executeCommand(any(CommandBlockLogic.class), anyString());
        } finally {
            singleton.set(null, previousServer);
        }
    }
}
