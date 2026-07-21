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

package com.sk89q.worldedit.extent.clipboard.io;

import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collection of supported clipboard formats.
 */
public enum ClipboardFormat {

    /**
     * The Schematic format used by many software.
     */
    SCHEMATIC("schematic", new String[] { "schematic" }, "mcedit", "mce", "schematic") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new SchematicReader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            NBTOutputStream nbtStream = new NBTOutputStream(new GZIPOutputStream(outputStream));
            return new SchematicWriter(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            return hasCompressedNbtRoot(file, "Schematic");
        }
    },

    /**
     * The WDG registry-stable schematic version 1 format.
     */
    WDG_SCHEMATIC("wdgschem", new String[] { "wdgschem" }, "wdg", "wdgschem") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new WdgSchematicReader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            NBTOutputStream nbtStream = new NBTOutputStream(new GZIPOutputStream(outputStream));
            return new WdgSchematicWriter(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            return hasCompressedNbtRoot(file, WdgSchematicFormat.ROOT_NAME);
        }
    };

    private static final Map<String, ClipboardFormat> aliasMap = new HashMap<String, ClipboardFormat>();

    private final String primaryExtension;
    private final String[] fileExtensions;
    private final String[] aliases;

    /**
     * Create a new instance.
     *
     * @param primaryExtension the extension appended when one is omitted
     * @param fileExtensions extensions advertised to file dialogs
     * @param aliases aliases by which this format may be referred to
     */
    private ClipboardFormat(String primaryExtension, String[] fileExtensions, String... aliases) {
        this.primaryExtension = primaryExtension;
        this.fileExtensions = fileExtensions.clone();
        this.aliases = aliases;
    }

    /**
     * Get a set of aliases.
     *
     * @return a set of aliases
     */
    public Set<String> getAliases() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(aliases)));
    }

    /**
     * Get the extension appended when a filename has no extension.
     *
     * @return the primary extension without a leading period
     */
    public String getPrimaryExtension() {
        return primaryExtension;
    }

    /**
     * Get the extensions advertised for this format.
     *
     * @return a defensive copy of extensions without leading periods
     */
    public String[] getFileExtensions() {
        return fileExtensions.clone();
    }

    /**
     * Create a reader.
     *
     * @param inputStream the input stream
     * @return a reader
     * @throws IOException thrown on I/O error
     */
    public abstract ClipboardReader getReader(InputStream inputStream) throws IOException;

    /**
     * Create a writer.
     *
     * @param outputStream the output stream
     * @return a writer
     * @throws IOException thrown on I/O error
     */
    public abstract ClipboardWriter getWriter(OutputStream outputStream) throws IOException;

    /**
     * Return whether the given file is of this format.
     *
     * @param file the file
     * @return true if the given file is of this format
     */
    public abstract boolean isFormat(File file);

    static {
        for (ClipboardFormat format : EnumSet.allOf(ClipboardFormat.class)) {
            for (String key : format.aliases) {
                aliasMap.put(key, format);
            }
        }
    }

    /**
     * Find the clipboard format named by the given alias.
     *
     * @param alias the alias
     * @return the format, otherwise null if none is matched
     */
    @Nullable
    public static ClipboardFormat findByAlias(String alias) {
        checkNotNull(alias);
        return aliasMap.get(alias.toLowerCase().trim());
    }

    /**
     * Detect the format given a file.
     *
     * @param file the file
     * @return the format, otherwise null if one cannot be detected
     */
    @Nullable
    public static ClipboardFormat findByFile(File file) {
        checkNotNull(file);

        for (ClipboardFormat format : EnumSet.allOf(ClipboardFormat.class)) {
            if (format.isFormat(file)) {
                return format;
            }
        }

        return null;
    }

    private static boolean hasCompressedNbtRoot(File file, String expectedRoot) {
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(new GZIPInputStream(new FileInputStream(file)));
            if ((stream.readByte() & 0xFF) != NBTConstants.TYPE_COMPOUND) {
                return false;
            }
            byte[] nameBytes = new byte[stream.readShort() & 0xFFFF];
            stream.readFully(nameBytes);
            String name = new String(nameBytes, NBTConstants.CHARSET);
            return expectedRoot.equals(name);
        } catch (IOException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
