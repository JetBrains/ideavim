/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.*;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class MapHandler extends CommandHandler implements VimScriptCommandHandler {
  public static final char CTRL_V = '\u0016';
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
  public static final EnumSet<SpecialArgument> UNSUPPORTED_SPECIAL_ARGUMENTS = EnumSet.of(
    SpecialArgument.EXPR,
    SpecialArgument.SCRIPT);

  public MapHandler() {
    super(COMMAND_NAMES, RANGE_FORBIDDEN | ARGUMENT_OPTIONAL);
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
    final CommandInfo commandInfo = getCommandInfo(cmd.getCommand());
    if (commandInfo != null) {
      final String argument = cmd.getArgument();
      final Set<MappingMode> modes = commandInfo.getMappingModes();
      if (argument.isEmpty()) {
        return editor != null && VimPlugin.getKey().showKeyMappings(modes, editor);
      }
      else {
        final CommandArguments arguments;
        try {
          arguments = parseCommandArguments(argument);
        }
        catch (IllegalArgumentException ignored) {
          return false;
        }
        if (arguments != null) {
          for (SpecialArgument unsupportedArgument : UNSUPPORTED_SPECIAL_ARGUMENTS) {
            if (arguments.getSpecialArguments().contains(unsupportedArgument)) {
              throw new ExException("Unsupported map argument: " + unsupportedArgument);
            }
          }
          VimPlugin.getKey().putKeyMapping(modes, arguments.getFromKeys(), arguments.getToKeys(), null,
                                           commandInfo.isRecursive());
          return true;

        }
      }
    }
    return false;
  }

  @Nullable
  private static CommandArguments parseCommandArguments(@NotNull String input) {
    input = getFirstBarSeparatedCommand(input);

    final Set<SpecialArgument> specialArguments = new HashSet<SpecialArgument>();
    final StringBuilder toKeysBuilder = new StringBuilder();
    List<KeyStroke> fromKeys = null;
    for (String part : input.split(" ")) {
      if (fromKeys != null) {
        toKeysBuilder.append(" ");
        toKeysBuilder.append(part);
      }
      else {
        final SpecialArgument specialArgument = SpecialArgument.fromString(part);
        if (specialArgument != null) {
          specialArguments.add(specialArgument);
        }
        else {
          fromKeys = parseKeys(part);
        }
      }
    }
    for (int i = input.length() - 1; i >= 0; i--) {
      final char c = input.charAt(i);
      if (c == ' ') {
        toKeysBuilder.append(c);
      }
      else {
        break;
      }
    }
    if (fromKeys != null) {
      final List<KeyStroke> toKeys = parseKeys(StringUtil.trimLeading(toKeysBuilder.toString()));
      return new CommandArguments(specialArguments, fromKeys, toKeys);

    }
    return null;
  }

  @NotNull
  private static String getFirstBarSeparatedCommand(@NotNull String input) {
    final StringBuilder inputBuilder = new StringBuilder();
    boolean escape = false;
    for (int i = 0; i < input.length(); i++) {
      final char c = input.charAt(i);
      if (escape) {
        escape = false;
        if (c != '|') {
          inputBuilder.append('\\');
        }
        inputBuilder.append(c);
      }
      else if (c == '\\' || c == CTRL_V) {
        escape = true;
      }
      else if (c == '|') {
        break;
      }
      else {
        inputBuilder.append(c);
      }
    }
    if (input.endsWith("\\")) {
      inputBuilder.append("\\");
    }
    return inputBuilder.toString();
  }

  @Nullable
  private static CommandInfo getCommandInfo(@NotNull String command) {
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

  private enum SpecialArgument {
    BUFFER("<buffer>"),
    NOWAIT("<nowait>"),
    SILENT("<silent>"),
    SPECIAL("<special>"),
    SCRIPT("<script>"),
    EXPR("<expr>"),
    UNIQUE("<unique>");

    @NotNull private final String myName;

    SpecialArgument(@NotNull String name) {
      myName = name;
    }

    @Nullable
    public static SpecialArgument fromString(@NotNull String s) {
      for (SpecialArgument argument : SpecialArgument.values()) {
        if (s.equals(argument.myName)) {
          return argument;
        }
      }
      return null;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    @NotNull
    @Override
    public String toString() {
      return myName;
    }
  }

  private static class CommandArguments {
    @NotNull private final Set<SpecialArgument> mySpecialArguments;
    @NotNull private final List<KeyStroke> myFromKeys;
    @NotNull private final List<KeyStroke> myToKeys;

    public CommandArguments(@NotNull Set<SpecialArgument> specialArguments, @NotNull List<KeyStroke> fromKeys,
                            @NotNull List<KeyStroke> toKeys) {

      mySpecialArguments = specialArguments;
      myFromKeys = fromKeys;
      myToKeys = toKeys;
    }

    @NotNull
    public Set<SpecialArgument> getSpecialArguments() {
      return mySpecialArguments;
    }

    @NotNull
    public List<KeyStroke> getFromKeys() {
      return myFromKeys;
    }

    @NotNull
    public List<KeyStroke> getToKeys() {
      return myToKeys;
    }
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
