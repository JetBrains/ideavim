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
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.VimrcCommandHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class MapHandler extends CommandHandler implements VimrcCommandHandler {
  public static final CommandInfo[] COMMAND_INFOS = new CommandInfo[] {
    // TODO: Support xmap, smap, map!, lmap
    new CommandInfo("map", "", MappingMode.NVO, true),
    new CommandInfo("nm", "ap", MappingMode.N, true),
    new CommandInfo("vm", "ap", MappingMode.V, true),
    new CommandInfo("om", "ap", MappingMode.O, true),
    new CommandInfo("im", "ap", MappingMode.I, true),
    new CommandInfo("cm", "ap", MappingMode.C, true),
    // TODO: Support xnoremap, snoremap, noremap!, lnoremap
    new CommandInfo("no", "remap", MappingMode.NVO, false),
    new CommandInfo("nn", "oremap", MappingMode.N, false),
    new CommandInfo("vn", "oremap", MappingMode.V, false),
    new CommandInfo("ono", "remap", MappingMode.O, false),
    new CommandInfo("ino", "remap", MappingMode.I, false),
    new CommandInfo("cno", "remap", MappingMode.C, false),
  };
  public static final CommandName[] COMMAND_NAMES = createCommandNames();

  private static final Pattern RE_MAP_ARGUMENTS = Pattern.compile("([^ ]+) +(.+)");

  public MapHandler() {
    super(COMMAND_NAMES, RANGE_FORBIDDEN | ARGUMENT_OPTIONAL);
  }

  @Override
  public boolean execute(@NotNull Editor editor, @NotNull DataContext context, @NotNull ExCommand cmd) {
    return executeCommand(cmd, editor);
  }

  @Override
  public void execute(@NotNull ExCommand cmd) {
    executeCommand(cmd, null);
  }

  private boolean executeCommand(@NotNull ExCommand cmd, @Nullable Editor editor) {
    final CommandInfo commandInfo = getCommandInfo(cmd.getCommand());
    if (commandInfo != null) {
      final String argument = cmd.getArgument();
      final Set<MappingMode> modes = commandInfo.getMappingModes();
      if (argument.isEmpty()) {
        return editor != null && VimPlugin.getKey().showKeyMappings(modes, editor);
      }
      else {
        final Matcher matcher = RE_MAP_ARGUMENTS.matcher(argument);
        if (matcher.matches()) {
          VimPlugin.getKey().putKeyMapping(modes, parseKeys(matcher.group(1)), parseKeys(matcher.group(2)),
                                           commandInfo.isRecursive());
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private CommandInfo getCommandInfo(@NotNull String command) {
    for (CommandInfo commandInfo : COMMAND_INFOS) {
      if (command.startsWith(commandInfo.getPrefix())) {
        return commandInfo;
      }
    }
    return null;
  }

  @NotNull
  private static CommandName[] createCommandNames() {
    final List<CommandName> commandNames = new ArrayList<CommandName>();
    for (CommandInfo commandInfo : COMMAND_INFOS) {
      commandNames.add(new CommandName(commandInfo.getPrefix(), commandInfo.getSuffix()));
    }
    return commandNames.toArray(new CommandName[commandNames.size()]);
  }

  private static class CommandInfo {
    @NotNull private final String myPrefix;
    @NotNull private final String mySuffix;
    @NotNull private final Set<MappingMode> myMappingModes;
    private final boolean myRecursive;

    public CommandInfo(@NotNull String prefix, @NotNull String suffix, @NotNull Set<MappingMode> mappingModes,
                       boolean recursive) {
      myPrefix = prefix;
      mySuffix = suffix;
      myMappingModes = mappingModes;
      myRecursive = recursive;
    }

    @NotNull
    public String getPrefix() {
      return myPrefix;
    }

    @NotNull
    public String getSuffix() {
      return mySuffix;
    }

    @NotNull
    public Set<MappingMode> getMappingModes() {
      return myMappingModes;
    }

    public boolean isRecursive() {
      return myRecursive;
    }
  }
}
