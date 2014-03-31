/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.key.KeyMapping;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class MapHandler extends CommandHandler {
  public static final Pattern RE_MAP_ARGUMENTS = Pattern.compile("([^ ]+) +(.+)");

  public static Map<String, Set<MappingMode>> MAPPING_MODE_NAMES = ImmutableMap.<String, Set<MappingMode>>builder()
    .put("nmap", MappingMode.N)
    .build();

  public MapHandler() {
    super(new CommandName[]{
      new CommandName("map", ""),
      new CommandName("nm", "ap")
      // TODO: Add other mapping commands
    }, RANGE_FORBIDDEN | ARGUMENT_OPTIONAL);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final Set<MappingMode> modes = MAPPING_MODE_NAMES.get(cmd.getCommand());
    if (modes != null) {
      final String argument = cmd.getArgument();
      if (argument.isEmpty()) {
        // TODO: Show key mapping table
        throw new UnsupportedOperationException(String.format("Key mapping table is not implemented yet"));
      }
      else {
        final Matcher matcher = RE_MAP_ARGUMENTS.matcher(argument);
        if (matcher.matches()) {
          final List<KeyStroke> leftKeys = parseKeys(matcher.group(1));
          final List<KeyStroke> rightKeys = parseKeys(matcher.group(2));
          for (MappingMode mode : modes) {
            final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
            mapping.put(leftKeys, rightKeys);
          }
          return true;
        }
      }
    }
    return false;
  }
}
