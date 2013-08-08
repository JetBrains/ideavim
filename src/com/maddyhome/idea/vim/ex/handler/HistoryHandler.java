/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.HistoryGroup;
import com.maddyhome.idea.vim.ui.MorePanel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 */
public class HistoryHandler extends CommandHandler {
  public HistoryHandler() {
    super("his", "tory", RANGE_FORBIDDEN | ARGUMENT_OPTIONAL | KEEP_FOCUS);
  }

  public boolean execute(@NotNull Editor editor, final DataContext context, @NotNull ExCommand cmd) throws ExException {
    logger.debug("execute");

    String arg = cmd.getArgument().trim();

    if (arg.length() == 0) {
      arg = "cmd";
      logger.debug("default to cmd");
    }

    String key;
    int spos = arg.indexOf(' ');
    if (spos >= 0) {
      key = arg.substring(0, spos).trim();
      arg = arg.substring(spos + 1);
    }
    else {
      key = arg;
      arg = "";
    }

    if (logger.isDebugEnabled()) {
      logger.debug("key='" + key + "'");
    }

    if (key.length() == 1 && ":/=@".indexOf(key.charAt(0)) >= 0) {
      switch (key.charAt(0)) {
        case ':':
          key = "cmd";
          break;
        case '/':
          key = "search";
          break;
        case '=':
          key = "expr";
          break;
        case '@':
          key = "input";
          break;
      }
    }
    else if (Character.isLetter(key.charAt(0))) {
      if (!"cmd".startsWith(key) &&
          !"search".startsWith(key) &&
          !"expr".startsWith(key) &&
          !"input".startsWith(key) &&
          !"all".startsWith(key)) {
        // Invalid command
        if (logger.isDebugEnabled()) {
          logger.debug("invalid command " + key);
        }
        return false;
      }
    }
    else {
      arg = key + " " + arg;
      key = "cmd";
    }

    String first;
    String last;
    int cpos = arg.indexOf(',');
    if (cpos >= 0) {
      first = arg.substring(0, cpos).trim();
      last = arg.substring(cpos + 1).trim();
    }
    else {
      first = arg;
      last = "";
    }

    int f = 0;
    int l = 0;
    try {
      if (first.length() > 0) {
        f = Integer.parseInt(first);
      }
      if (last.length() > 0) {
        l = Integer.parseInt(last);
      }
    }
    catch (NumberFormatException e) {
      logger.debug("bad number");
      // Oops - bad range
      return false;
    }

    StringBuffer res = new StringBuffer();
    switch (key.charAt(0)) {
      case 'c':
        res.append(processKey(HistoryGroup.COMMAND, f, l));
        break;
      case 's':
        res.append(processKey(HistoryGroup.SEARCH, f, l));
        break;
      case 'e':
        res.append(processKey(HistoryGroup.EXPRESSION, f, l));
        break;
      case 'i':
        res.append(processKey(HistoryGroup.INPUT, f, l));
        break;
      case 'a':
        res.append(processKey(HistoryGroup.COMMAND, f, l));
        res.append(processKey(HistoryGroup.SEARCH, f, l));
        res.append(processKey(HistoryGroup.EXPRESSION, f, l));
        res.append(processKey(HistoryGroup.INPUT, f, l));
        break;
    }

    MorePanel panel = MorePanel.getInstance(editor);
    panel.setText(res.toString());

    return true;
  }

  @NotNull
  private String processKey(String key, int start, int end) {
    if (logger.isDebugEnabled()) {
      logger.debug("process " + key + " " + start + "," + end);
    }

    StringBuffer res = new StringBuffer();

    res.append("      #  ").append(key).append(" ").append("history\n");

    String spaces = "       ";
    List<HistoryGroup.HistoryEntry> entries = CommandGroups.getInstance().getHistory().getEntries(key, start, end);
    for (HistoryGroup.HistoryEntry entry : entries) {
      String num = Integer.toString(entry.getNumber());
      res.append(spaces.substring(num.length())).append(num).append("  ").append(entry.getEntry()).append("\n");
    }

    return res.toString();
  }

  private static Logger logger = Logger.getInstance(HistoryHandler.class.getName());
}
