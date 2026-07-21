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

package com.sk89q.worldedit.util.command.parametric;

import com.sk89q.minecraft.util.commands.CommandLocals;

import java.util.List;

/**
 * Optional binding extension for suggestions that require command context.
 */
public interface ContextualBinding extends Binding {

    /**
     * Get contextual suggestions for a parameter.
     *
     * <p>Return {@code null} when this binding has no parameter-specific
     * completer and the command's default completer should be used.</p>
     *
     * @param parameter parameter being completed
     * @param prefix current argument prefix
     * @param locals command locals
     * @return parameter-specific suggestions, an empty list for no matches, or null
     */
    List<String> getSuggestions(ParameterData parameter, String prefix, CommandLocals locals);

}
