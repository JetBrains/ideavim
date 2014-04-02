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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.key.KeyMapping;
import com.maddyhome.idea.vim.ui.MorePanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class MapHandler extends CommandHandler implements VimrcCommandHandler {
  public static final Pattern RE_MAP_ARGUMENTS = Pattern.compile("([^ ]+) +(.+)");

  public MapHandler() {
    super(new CommandName[]{
      // TODO: Support xmap, smap, lmap
      new CommandName("map", ""),
      new CommandName("nm", "ap"),
      new CommandName("vm", "ap"),
      new CommandName("om", "ap"),
      new CommandName("map!", ""),
      new CommandName("im", "ap"),
      new CommandName("cm", "ap")
    }, RANGE_FORBIDDEN | ARGUMENT_OPTIONAL | KEEP_FOCUS);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    return executeCommand(cmd, editor);
  }

  @Override
  public void execute(@NotNull ExCommand cmd) throws ExException {
    executeCommand(cmd, null);
  }

  private boolean executeCommand(@NotNull ExCommand cmd, @Nullable Editor editor) throws ExException {
    final Set<MappingMode> modes = getMappingModes(cmd.getCommand());
    if (modes != null) {
      final String argument = cmd.getArgument();
      if (argument.isEmpty()) {
        return editor != null && showMappings(modes, editor);
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

  private boolean showMappings(@NotNull Set<MappingMode> modes, @NotNull Editor editor) {
    final MorePanel panel = MorePanel.getInstance(editor);
    panel.setText("Key mapping table is not implemented yet");
    return true;
  }

  @Nullable
  private Set<MappingMode> getMappingModes(@NotNull String command) {
    if (command.equals("map")) {
      return MappingMode.NVO;
    }
    else if (command.startsWith("nm")) {
      return MappingMode.N;
    }
    else if (command.startsWith("vm")) {
      return MappingMode.V;
    }
    else if (command.startsWith("om")) {
      return MappingMode.O;
    }
    else if (command.equals("map!")) {
      return MappingMode.IC;
    }
    else if (command.startsWith("im")) {
      return MappingMode.I;
    }
    else if (command.startsWith("cm")) {
      return MappingMode.C;
    }
    return null;
  }
}
