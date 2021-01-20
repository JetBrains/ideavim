/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.text.CharSequenceReader;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.EnumSet;


public class ProcessGroup {
  public String getLastCommand() {
    return lastCommand;
  }

  public void startSearchCommand(@NotNull Editor editor, DataContext context, int count, char leader) {
    if (editor.isOneLineMode()) // Don't allow searching in one line editors
    {
      return;
    }

    String initText = "";
    String label = String.valueOf(leader);

    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(editor, context, label, initText, count);
  }

  public boolean isForwardSearch() {
    return ExEntryPanel.getInstance().getLabel().equals("/");
  }

  public @NotNull String endSearchCommand(final @NotNull Editor editor) {
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate(true);

    return panel.getText();
  }

  public void startExCommand(@NotNull Editor editor, DataContext context, @NotNull Command cmd) {
    // Don't allow ex commands in one line editors
    if (editor.isOneLineMode()) return;

    String initText = getRange(editor, cmd);
    CommandState.getInstance(editor).pushModes(CommandState.Mode.CMD_LINE, CommandState.SubMode.NONE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(editor, context, ":", initText, 1);
  }

  public boolean processExKey(Editor editor, @NotNull KeyStroke stroke) {
    // This will only get called if somehow the key focus ended up in the editor while the ex entry window
    // is open. So I'll put focus back in the editor and process the key.

    ExEntryPanel panel = ExEntryPanel.getInstance();
    if (panel.isActive()) {
      UiHelper.requestFocus(panel.getEntry());
      panel.handleKey(stroke);

      return true;
    }
    else {
      CommandState.getInstance(editor).popModes();
      KeyHandler.getInstance().reset(editor);
      return false;
    }
  }

  public boolean processExEntry(final @NotNull Editor editor, final @NotNull DataContext context) {
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate(true);
    boolean res = true;
    try {
      CommandState.getInstance(editor).popModes();
      logger.debug("processing command");
      final String text = panel.getText();
      if (logger.isDebugEnabled()) logger.debug("swing=" + SwingUtilities.isEventDispatchThread());
      if (panel.getLabel().equals(":")) {
        CommandParser.getInstance().processCommand(editor, context, text, 1);
      }
      else {
        // FIXME looks like this branch gets never executed
        // Search is handled through SearchEntry(Fwd|Rev)Action waiting for an argument type of EX_STRING. Once ex entry
        // is complete, ProcessExEntryAction should be invoked which would invoke this method. However, keyHandler
        // massages the Command stack, ignores ProcessExEntryAction, passes the ex content as a string argument to
        // the previous SearchEntry(Fwd|Rev)Action and invokes it. This works better because the argument text is saved
        // for repeats, and any leading operators are also executed (e.g. "d/foo")
        int pos = VimPlugin.getSearch().search(editor, text, panel.getCount(),
                                                                 panel.getLabel().equals("/")
                                                                 ? EnumSet.of(CommandFlags.FLAG_SEARCH_FWD)
                                                                 : EnumSet.of(CommandFlags.FLAG_SEARCH_REV), true);
        if (pos == -1) {
          res = false;
        }
      }
    }
    catch (ExException e) {
      VimPlugin.showMessage(e.getMessage());
      VimPlugin.indicateError();
      res = false;
    }
    catch (Exception bad) {
      ProcessGroup.logger.error(bad);
      VimPlugin.indicateError();
      res = false;
    }

    return res;
  }

  public void cancelExEntry(final @NotNull Editor editor, boolean resetCaret) {
    CommandState.getInstance(editor).popModes();
    KeyHandler.getInstance().reset(editor);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate(true, resetCaret);
  }

  public void startFilterCommand(@NotNull Editor editor, DataContext context, @NotNull Command cmd) {
    String initText = getRange(editor, cmd) + "!";
    CommandState.getInstance(editor).pushModes(CommandState.Mode.CMD_LINE, CommandState.SubMode.NONE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(editor, context, ":", initText, 1);
  }

  private @NotNull String getRange(Editor editor, @NotNull Command cmd) {
    String initText = "";
    if (CommandState.getInstance(editor).getMode() == CommandState.Mode.VISUAL) {
      initText = "'<,'>";
    }
    else if (cmd.getRawCount() > 0) {
      if (cmd.getCount() == 1) {
        initText = ".";
      }
      else {
        initText = ".,.+" + (cmd.getCount() - 1);
      }
    }

    return initText;
  }

  public boolean executeFilter(@NotNull Editor editor, @NotNull TextRange range,
                               @NotNull String command) throws IOException {
    final CharSequence charsSequence = editor.getDocument().getCharsSequence();
    final int startOffset = range.getStartOffset();
    final int endOffset = range.getEndOffset();
    final String output = executeCommand(command, charsSequence.subSequence(startOffset, endOffset));
    editor.getDocument().replaceString(startOffset, endOffset, output);
    return true;
  }

  public @NotNull String executeCommand(@NotNull String command, @Nullable CharSequence input) throws IOException {
    if (logger.isDebugEnabled()) {
      logger.debug("command=" + command);
    }

    final Process process = Runtime.getRuntime().exec(command);

    if (input != null) {
      final BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      copy(new CharSequenceReader(input), outputWriter);
      outputWriter.close();
    }

    final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    final StringWriter writer = new StringWriter();
    copy(inputReader, writer);
    writer.close();

    lastCommand = command;
    return writer.toString();
  }

  private void copy(@NotNull Reader from, @NotNull Writer to) throws IOException {
    char[] buf = new char[2048];
    int cnt;
    while ((cnt = from.read(buf)) != -1) {
      to.write(buf, 0, cnt);
    }
  }

  private String lastCommand;

  private static final Logger logger = Logger.getInstance(ProcessGroup.class.getName());
}
