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

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.util.auth.Authorizer;
import com.sk89q.worldedit.util.command.CommandCompleter;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.binding.Switch;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParametricSuggestionTest {

    private SimpleDispatcher dispatcher;
    private CommandLocals locals;

    @Before
    public void setUp() throws ParametricException {
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new LocalAuthorizer());
        builder.setDefaultCompleter(new CommandCompleter() {
            @Override
            public List<String> getSuggestions(String arguments, CommandLocals locals) {
                return Collections.singletonList("default-user");
            }
        });
        builder.addBinding(new TestBinding());

        dispatcher = new SimpleDispatcher();
        builder.registerMethodsAsCommands(dispatcher, new TestCommands());
        locals = new CommandLocals();
        locals.put("allowed", Boolean.TRUE);
    }

    @Test
    public void permissionGrantedUsesParameterSuggestions() throws CommandException {
        assertEquals(Collections.singletonList("right:mine"),
                dispatcher.getSuggestions("optional mine", locals));
    }

    @Test
    public void permissionDeniedReturnsNoArgumentSuggestions() throws CommandException {
        locals.put("allowed", Boolean.FALSE);
        assertTrue(dispatcher.getSuggestions("optional mine", locals).isEmpty());
    }

    @Test
    public void parameterSpecificEmptyResultsDoNotFallBackToUsers() throws CommandException {
        assertTrue(dispatcher.getSuggestions("optional missing", locals).isEmpty());
    }

    @Test
    public void defaultCompleterRemainsForUnrelatedParameters() throws CommandException {
        assertEquals(Collections.singletonList("default-user"),
                dispatcher.getSuggestions("plain player", locals));
    }

    @Test
    public void optionalParameterIsLocatedBeforeRequiredParameter() throws CommandException {
        assertEquals(Collections.singletonList("right:"),
                dispatcher.getSuggestions("optional ", locals));
        assertEquals(Collections.singletonList("right:second"),
                dispatcher.getSuggestions("optional first second", locals));
    }

    @Test
    public void flagsDoNotMoveTheActiveParameter() throws CommandException {
        assertEquals(Collections.singletonList("left:mine"),
                dispatcher.getSuggestions("flagged -h mine", locals));
    }

    @Test
    public void multipleArgumentsCompleteIndependently() throws CommandException {
        assertEquals(Collections.singletonList("left:first"),
                dispatcher.getSuggestions("multiple first", locals));
        assertEquals(Collections.singletonList("right:second"),
                dispatcher.getSuggestions("multiple first second", locals));
    }

    private static class TestCommands {

        @Command(aliases = { "optional" }, desc = "optional")
        @CommandPermissions("allowed")
        public void optional(@Optional LeftValue left, RightValue right) {
        }

        @Command(aliases = { "flagged" }, desc = "flagged")
        @CommandPermissions("allowed")
        public void flagged(@Switch('h') boolean hollow, LeftValue value) {
        }

        @Command(aliases = { "multiple" }, desc = "multiple")
        @CommandPermissions("allowed")
        public void multiple(LeftValue left, RightValue right) {
        }

        @Command(aliases = { "plain" }, desc = "plain")
        @CommandPermissions("allowed")
        public void plain(String value) {
        }

    }

    private static class LeftValue {
    }

    private static class RightValue {
    }

    private static class TestBinding implements ContextualBinding {

        @Override
        public Type[] getTypes() {
            return new Type[] { LeftValue.class, RightValue.class };
        }

        @Override
        public BindingBehavior getBehavior(ParameterData parameter) {
            return BindingBehavior.CONSUMES;
        }

        @Override
        public int getConsumedCount(ParameterData parameter) {
            return 1;
        }

        @Override
        public Object bind(ParameterData parameter, ArgumentStack scoped, boolean onlyConsume)
                throws ParameterException, CommandException, InvocationTargetException {
            scoped.next();
            return LeftValue.class.equals(parameter.getType()) ? new LeftValue() : new RightValue();
        }

        @Override
        public List<String> getSuggestions(ParameterData parameter, String prefix) {
            return Collections.emptyList();
        }

        @Override
        public List<String> getSuggestions(ParameterData parameter, String prefix, CommandLocals locals) {
            if ("missing".equals(prefix)) {
                return Collections.emptyList();
            }
            String type = LeftValue.class.equals(parameter.getType()) ? "left:" : "right:";
            return Collections.singletonList(type + prefix);
        }

    }

    private static class LocalAuthorizer implements Authorizer {

        @Override
        public boolean testPermission(CommandLocals locals, String permission) {
            return Boolean.TRUE.equals(locals.get(permission));
        }

    }

}
