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

package com.sk89q.worldedit.internal.command;

import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistryNameCompleter;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Conservative block registry-name completion shared by command systems.
 */
public final class BlockRegistryCompletion {

    public static final int MAX_RESULTS = 100;

    private BlockRegistryCompletion() {
    }

    /**
     * Complete a single block argument using the actor's active world registry.
     *
     * @param locals command locals
     * @param input current argument
     * @return suggestions
     */
    public static List<String> getBlockSuggestions(CommandLocals locals, String input) {
        return getBlockSuggestions(getBlockRegistry(locals), input, MAX_RESULTS);
    }

    /**
     * Complete a pattern argument using the actor's active world registry.
     *
     * @param locals command locals
     * @param input current argument
     * @return suggestions
     */
    public static List<String> getPatternSuggestions(CommandLocals locals, String input) {
        return getPatternSuggestions(getBlockRegistry(locals), input, MAX_RESULTS);
    }

    /**
     * Complete a single block argument against a supplied registry.
     *
     * @param registry block registry
     * @param input current argument
     * @param limit maximum results
     * @return suggestions
     */
    public static List<String> getBlockSuggestions(BlockRegistry registry, String input, int limit) {
        MetadataSegment metadata = parseMetadataSegment(registry, input);
        if (metadata != null) {
            return completeMetadata("", metadata, limit);
        }
        if (!isSafeRegistryPrefix(input)) {
            return Collections.emptyList();
        }
        return complete(registry, "", input, limit);
    }

    /**
     * Complete the active trailing registry-name segment in a simple pattern.
     *
     * @param registry block registry
     * @param input current argument
     * @param limit maximum results
     * @return suggestions
     */
    public static List<String> getPatternSuggestions(BlockRegistry registry, String input, int limit) {
        PatternSegment segment = parsePatternSegment(input);
        if (segment == null) {
            return Collections.emptyList();
        }
        MetadataSegment metadata = parseMetadataSegment(registry, segment.prefix);
        if (metadata != null) {
            return completeMetadata(segment.preserved, metadata, limit);
        }
        if (!isSafeRegistryPrefix(segment.prefix)) {
            return Collections.emptyList();
        }
        return complete(registry, segment.preserved, segment.prefix, limit);
    }

    private static List<String> completeMetadata(String preserved, MetadataSegment metadata, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        List<String> suggestions = new ArrayList<String>();
        for (int data = 0; data <= 15 && suggestions.size() < limit; data++) {
            String value = Integer.toString(data);
            if (value.startsWith(metadata.dataPrefix)) {
                suggestions.add(preserved + metadata.block + ":" + value);
            }
        }

        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(suggestions);
    }

    private static List<String> complete(BlockRegistry registry, String preserved, String prefix, int limit) {
        if (!(registry instanceof BlockRegistryNameCompleter) || limit <= 0) {
            return Collections.emptyList();
        }

        List<String> registrySuggestions = ((BlockRegistryNameCompleter) registry)
                .getRegistryNameSuggestions(prefix, limit);
        if (registrySuggestions == null || registrySuggestions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> unique = new LinkedHashSet<String>();
        for (String suggestion : registrySuggestions) {
            if (suggestion != null && unique.size() < limit) {
                unique.add(preserved + suggestion);
            }
        }

        if (unique.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(unique));
    }

    private static BlockRegistry getBlockRegistry(CommandLocals locals) {
        if (locals == null) {
            return null;
        }

        Actor actor = locals.get(Actor.class);
        if (!(actor instanceof Entity)) {
            return null;
        }

        Extent extent = ((Entity) actor).getExtent();
        if (!(extent instanceof World)) {
            return null;
        }

        return ((World) extent).getWorldData().getBlockRegistry();
    }

    private static PatternSegment parsePatternSegment(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        int segmentStart = input.lastIndexOf(',') + 1;
        String completed = input.substring(0, segmentStart);
        String active = input.substring(segmentStart);

        if (!completed.isEmpty()) {
            String[] entries = completed.substring(0, completed.length() - 1).split(",", -1);
            for (String entry : entries) {
                if (!isSafeCompletedEntry(entry)) {
                    return null;
                }
            }
        }

        int percentage = active.indexOf('%');
        String weight = "";
        if (percentage >= 0) {
            if (percentage != active.lastIndexOf('%')) {
                return null;
            }
            String number = active.substring(0, percentage);
            if (!isPercentage(number)) {
                return null;
            }
            weight = active.substring(0, percentage + 1);
            active = active.substring(percentage + 1);
        }

        if (!isSafeRegistryOrMetadataPrefix(active)) {
            return null;
        }

        return new PatternSegment(completed + weight, active);
    }

    private static boolean isSafeCompletedEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }

        int percentage = entry.indexOf('%');
        if (percentage >= 0) {
            if (percentage != entry.lastIndexOf('%') || !isPercentage(entry.substring(0, percentage))) {
                return false;
            }
            entry = entry.substring(percentage + 1);
        }

        if (entry.isEmpty()) {
            return false;
        }

        int colonCount = count(entry, ':');
        if (colonCount > 2) {
            return false;
        }
        return isSafeToken(entry);
    }

    private static boolean isSafeRegistryPrefix(String value) {
        if (value == null || value.isEmpty() || count(value, ':') > 1) {
            return false;
        }
        return isSafeToken(value);
    }

    private static boolean isSafeRegistryOrMetadataPrefix(String value) {
        if (value == null || value.isEmpty() || count(value, ':') > 2) {
            return false;
        }
        return isSafeToken(value);
    }

    private static MetadataSegment parseMetadataSegment(BlockRegistry registry, String value) {
        if (!isSafeRegistryOrMetadataPrefix(value)) {
            return null;
        }

        int lastColon = value.lastIndexOf(':');
        if (lastColon <= 0) {
            return null;
        }

        String block = value.substring(0, lastColon);
        String dataPrefix = value.substring(lastColon + 1);
        if (!isNumericPrefix(dataPrefix) || !isCompleteBlockReference(registry, block)) {
            return null;
        }

        return new MetadataSegment(block, dataPrefix);
    }

    private static boolean isCompleteBlockReference(BlockRegistry registry, String block) {
        if (block.indexOf(':') >= 0) {
            return registry instanceof BlockRegistryNameResolver
                    && ((BlockRegistryNameResolver) registry).getBlockId(block) != null;
        }

        return BlockType.lookup(block.replace('_', ' '), false) != null;
    }

    private static boolean isNumericPrefix(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isSafeToken(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if ((character >= 'a' && character <= 'z')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= '0' && character <= '9')
                    || character == '_'
                    || character == '-'
                    || character == '.'
                    || character == '/'
                    || character == ':') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isPercentage(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        boolean decimal = false;
        boolean digit = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character >= '0' && character <= '9') {
                digit = true;
            } else if (character == '.' && !decimal) {
                decimal = true;
            } else {
                return false;
            }
        }
        return digit;
    }

    private static int count(String value, char character) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }

    private static class PatternSegment {

        private final String preserved;
        private final String prefix;

        PatternSegment(String preserved, String prefix) {
            this.preserved = preserved;
            this.prefix = prefix;
        }

    }

    private static class MetadataSegment {

        private final String block;
        private final String dataPrefix;

        MetadataSegment(String block, String dataPrefix) {
            this.block = block;
            this.dataPrefix = dataPrefix;
        }

    }

}
