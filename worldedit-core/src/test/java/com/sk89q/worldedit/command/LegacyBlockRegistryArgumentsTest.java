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

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.worldedit.internal.command.BlockRegistryArguments;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class LegacyBlockRegistryArgumentsTest {

    @Test
    public void utilityCommandsDeclareRawPatternPositions() {
        assertPatterns(UtilityCommands.class, "/fill", 0);
        assertPatterns(UtilityCommands.class, "/fillr", 0);
        assertBlocks(UtilityCommands.class, "/removenear", 0);
        assertPatterns(UtilityCommands.class, "/replacenear", 1, 2);
    }

    @Test
    public void toolCommandsDeclareSingleAndMultipleBlockPositions() {
        assertBlocks(ToolCommands.class, "repl", 0);
        assertPatterns(ToolCommands.class, "floodfill", 0);
        assertBlocks(ToolCommands.class, "lrbuild", 0, 1);
    }

    @Test
    public void countCommandDeclaresItsBlockListPosition() {
        assertPatterns(SelectionCommands.class, "/count", 0);
    }

    private static void assertBlocks(Class<?> type, String alias, int... positions) {
        BlockRegistryArguments arguments = find(type, alias);
        assertArrayEquals(positions, arguments.blocks());
    }

    private static void assertPatterns(Class<?> type, String alias, int... positions) {
        BlockRegistryArguments arguments = find(type, alias);
        assertArrayEquals(positions, arguments.patterns());
    }

    private static BlockRegistryArguments find(Class<?> type, String alias) {
        for (Method method : type.getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command == null) {
                continue;
            }
            for (String candidate : command.aliases()) {
                if (alias.equals(candidate)) {
                    BlockRegistryArguments arguments = method.getAnnotation(BlockRegistryArguments.class);
                    assertNotNull("Missing registry completion metadata for " + alias, arguments);
                    return arguments;
                }
            }
        }
        throw new AssertionError("Command alias not found: " + alias);
    }

}
