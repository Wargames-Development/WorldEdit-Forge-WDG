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

package com.sk89q.worldedit.extension.factory;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockMaterial;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BlockRegistryNameResolver;
import com.sk89q.worldedit.world.registry.State;
import com.sk89q.worldedit.world.registry.WorldData;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultBlockParserTest {

    private DefaultBlockParser parser;
    private ParserContext context;

    @Before
    public void setUp() {
        TestRegistry registry = new TestRegistry();
        registry.register(0, "minecraft:air");
        registry.register(35, "minecraft:wool");
        registry.register(39, "minecraft:brown_mushroom");

        WorldData worldData = mock(WorldData.class);
        when(worldData.getBlockRegistry()).thenReturn(registry);

        World world = mock(World.class);
        when(world.getWorldData()).thenReturn(worldData);
        when(world.isValidBlockType(anyInt())).thenReturn(true);

        context = new ParserContext();
        context.setWorld(world);
        context.setActor(mock(Actor.class));
        context.setRestricted(false);

        parser = new DefaultBlockParser(null);
    }

    @Test
    public void parsesCanonicalRegistryNameContainingUnderscore() throws Exception {
        BaseBlock block = parser.parseFromInput("minecraft:brown_mushroom", context);

        assertEquals(39, block.getId());
        assertEquals(0, block.getData());
    }

    @Test
    public void parsesCanonicalRegistryNameWithMetadata() throws Exception {
        BaseBlock block = parser.parseFromInput("minecraft:wool:14", context);

        assertEquals(35, block.getId());
        assertEquals(14, block.getData());
    }

    @Test
    public void parsesCanonicalAirRegistryName() throws Exception {
        BaseBlock block = parser.parseFromInput("minecraft:air", context);

        assertEquals(0, block.getId());
        assertEquals(0, block.getData());
    }

    @Test
    public void retainsLegacyUnderscoreAliasNormalisation() throws Exception {
        BaseBlock block = parser.parseFromInput("stone_brick", context);

        assertEquals(BlockType.STONE_BRICK.getID(), block.getId());
    }

    private static class TestRegistry implements BlockRegistry, BlockRegistryNameResolver {

        private final Map<Integer, String> names = new HashMap<Integer, String>();
        private final Map<String, Integer> ids = new HashMap<String, Integer>();

        void register(int id, String name) {
            names.put(id, name);
            ids.put(name, id);
        }

        @Nullable
        @Override
        public String getRegistryName(int blockId) {
            return names.get(blockId);
        }

        @Nullable
        @Override
        public Integer getBlockId(String registryName) {
            return ids.get(registryName);
        }

        @Nullable
        @Override
        public BaseBlock createFromId(String id) {
            return null;
        }

        @Nullable
        @Override
        public BaseBlock createFromId(int id) {
            return null;
        }

        @Nullable
        @Override
        public BlockMaterial getMaterial(BaseBlock block) {
            return null;
        }

        @Nullable
        @Override
        public Map<String, ? extends State> getStates(BaseBlock block) {
            return null;
        }

    }

}
