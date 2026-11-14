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

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repairs only WorldEdit's Bukkit-to-Forge handoff on Crucible. No Bukkit API
 * references are linked on ordinary Forge, and no new command names are added.
 */
final class CrucibleCommandBridge {
    private CrucibleCommandBridge() {
    }

    static void install(ICommandManager manager) {
        Class<?> bukkit;
        try {
            bukkit = Class.forName("org.bukkit.Bukkit");
        } catch (ClassNotFoundException absent) {
            return;
        }
        try {
            Object server = bukkit.getMethod("getServer").invoke(null);
            if (server == null) return;
            Object commandMap = server.getClass().getMethod("getCraftCommandMap").invoke(server);
            int count = install(manager, commandMap, server.getClass().getPackage().getName());
            ForgeWorldEdit.logger.info("Installed Crucible slash-command bridge for " + count
                    + " WorldEdit commands");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            ForgeWorldEdit.logger.warn("Could not install the WorldEdit Crucible slash-command bridge", failure);
        }
    }

    static int install(ICommandManager manager, Object commandMap, String craftPackage)
            throws ReflectiveOperationException {
        ClassLoader loader = commandMap.getClass().getClassLoader();
        Class<?> modCommand = Class.forName(craftPackage + ".command.ModCustomCommand", false, loader);
        Class<?> vanillaWrapper = Class.forName(craftPackage + ".command.VanillaCommandWrapper", false, loader);
        Class<?> commandType = Class.forName("org.bukkit.command.Command", false, loader);
        Class<?> mapType = Class.forName("org.bukkit.command.CommandMap", false, loader);
        Constructor<?> constructor = vanillaWrapper.getConstructor(CommandBase.class);
        Method getName = commandType.getMethod("getName");
        Method getPermission = commandType.getMethod("getPermission");
        Method setPermission = commandType.getMethod("setPermission", String.class);
        Method register = commandType.getMethod("register", mapType);
        Map<String, Object> knownCommands = knownCommands(commandMap);
        Map<Object, Object> replacements = new IdentityHashMap<Object, Object>();
        for (Object original : knownCommands.values()) {
            if (original.getClass() != modCommand || replacements.containsKey(original)) continue;
            // Crucible stamps mod commands with their native class name. A lookup
            // by name alone could otherwise mistake a colliding mod's wrapper for ours.
            if (!CommandWrapper.class.getName().equals(getPermission.invoke(original))) continue;
            ICommand nativeCommand = (ICommand) manager.getCommands().get(getName.invoke(original));
            if (!(nativeCommand instanceof CommandWrapper) || !hasSlashAlias(nativeCommand)) continue;
            Object replacement = constructor.newInstance(new NativeCommandBridge(manager, nativeCommand));
            setPermission.invoke(replacement, getPermission.invoke(original));
            commandType.getMethod("setAliases", List.class).invoke(replacement,
                    commandType.getMethod("getAliases").invoke(original));
            commandType.getMethod("setDescription", String.class).invoke(replacement,
                    commandType.getMethod("getDescription").invoke(original));
            commandType.getMethod("setUsage", String.class).invoke(replacement,
                    commandType.getMethod("getUsage").invoke(original));
            register.invoke(replacement, commandMap);
            replacements.put(original, replacement);
        }
        // Replace by identity only after all adapters are ready, preserving existing
        // primary/namespaced aliases and the precedence of Bukkit plugins and mods.
        for (Map.Entry<String, Object> entry : knownCommands.entrySet()) {
            Object replacement = replacements.get(entry.getValue());
            if (replacement != null) entry.setValue(replacement);
        }
        return replacements.size();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> knownCommands(Object commandMap) throws ReflectiveOperationException {
        for (Class<?> type = commandMap.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, Object>) field.get(commandMap);
            } catch (NoSuchFieldException absent) {
                // Bukkit declares this field on SimpleCommandMap, not its Crucible subclass.
            }
        }
        throw new NoSuchFieldException("Bukkit command map has no knownCommands field");
    }

    private static boolean hasSlashAlias(ICommand command) {
        if (command.getCommandName().startsWith("/")) return true;
        for (Object alias : command.getCommandAliases()) {
            if (((String) alias).startsWith("/")) return true;
        }
        return false;
    }

    /** Bukkit's vanilla wrapper calls this directly, without stripping a slash. */
    static final class NativeCommandBridge extends CommandBase {
        private final ICommandManager manager;
        private final ICommand command;

        NativeCommandBridge(ICommandManager manager, ICommand command) {
            this.manager = manager;
            this.command = command;
        }

        @Override
        public String getCommandName() {
            return command.getCommandName();
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return command.getCommandUsage(sender);
        }

        @Override
        public List getCommandAliases() {
            return command.getCommandAliases();
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return command.canCommandSenderUseCommand(sender);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] arguments) {
            // Re-enter Forge with exactly one transport slash. Its normal event bus,
            // cancellation and WorldEdit permission checks still run once.
            manager.executeCommand(sender, "/" + CommandWrapper.getWorldEditCommandLine(command, arguments));
        }

        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] arguments) {
            return command.addTabCompletionOptions(sender, arguments);
        }
    }
}
