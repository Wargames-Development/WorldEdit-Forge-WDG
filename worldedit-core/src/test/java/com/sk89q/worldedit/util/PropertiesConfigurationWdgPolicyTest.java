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

package com.sk89q.worldedit.util;

import com.sk89q.worldedit.extent.clipboard.io.WdgTileEntityPolicy;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests configuration loading and canonical storage for the WDG save policy.
 */
public class PropertiesConfigurationWdgPolicyTest {

    @Test
    public void testMissingValueDefaultsToPreserveAndIsStored() throws Exception {
        assertPolicy(null, WdgTileEntityPolicy.PRESERVE, "preserve");
    }

    @Test
    public void testValidValuesAreTrimmedCaseInsensitiveAndCanonicalized() throws Exception {
        assertPolicy("  PrEsErVe  ", WdgTileEntityPolicy.PRESERVE, "preserve");
        assertPolicy(" STRIP ", WdgTileEntityPolicy.STRIP, "strip");
    }

    @Test
    public void testInvalidAndBooleanAliasesFallBackToPreserve() throws Exception {
        assertPolicy("maybe", WdgTileEntityPolicy.PRESERVE, "preserve");
        assertPolicy("true", WdgTileEntityPolicy.PRESERVE, "preserve");
        assertPolicy("1", WdgTileEntityPolicy.PRESERVE, "preserve");
    }

    private void assertPolicy(String configuredValue, WdgTileEntityPolicy expectedPolicy,
                              String expectedStoredValue) throws Exception {
        File directory = createTemporaryDirectory();
        File file = new File(directory, "worldedit.properties");
        try {
            if (configuredValue != null) {
                Properties input = new Properties();
                input.setProperty("wdg-schematic-tile-entity-policy", configuredValue);
                FileOutputStream output = new FileOutputStream(file);
                try {
                    input.store(output, "test");
                } finally {
                    output.close();
                }
            }

            PropertiesConfiguration configuration = new PropertiesConfiguration(file);
            configuration.load();
            assertEquals(expectedPolicy, configuration.wdgSchematicTileEntityPolicy);

            Properties stored = new Properties();
            FileInputStream input = new FileInputStream(file);
            try {
                stored.load(input);
            } finally {
                input.close();
            }
            assertEquals(expectedStoredValue,
                    stored.getProperty("wdg-schematic-tile-entity-policy"));
        } finally {
            deleteRecursively(directory);
        }
    }

    private File createTemporaryDirectory() throws Exception {
        File file = File.createTempFile("worldedit-wdg-policy-", "-test");
        assertTrue(file.delete());
        assertTrue(file.mkdir());
        return file;
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        assertTrue(file.delete());
    }
}
