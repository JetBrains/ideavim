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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.text.CharSequenceReader;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.InvalidCommandException;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.List;


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

      if (!panel.getLabel().equals(":")) {
        // Search is handled via Argument.Type.EX_STRING. Although ProcessExEntryAction is registered as the handler for
        // <CR> in both command and search modes, it's only invoked for command mode (see KeyHandler.handleCommandNode).
        // We should never be invoked for anything other than an actual ex command.
        throw new InvalidCommandException("Expected ':' command. Got '" + panel.getLabel() + "'", text);
      }

      if (logger.isDebugEnabled()) logger.debug("swing=" + SwingUtilities.isEventDispatchThread());

      CommandParser.INSTANCE.processCommand(editor, context, text, 1, false);
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

  public @Nullable String executeCommand(@NotNull Editor editor, @NotNull String command, @Nullable CharSequence input)
    throws ExecutionException, ProcessCanceledException {

    // TODO: This is much simpler than what Vim does. We should pass execution through the OS shell
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("command=" + command);
      }

      final List<String> commands = ParametersListUtil.parse(command, false, true);
      final GeneralCommandLine commandLine = new GeneralCommandLine(commands);
      final CapturingProcessHandler handler = new CapturingProcessHandler(commandLine);
      if (input != null) {
        handler.addProcessListener(new ProcessAdapter() {
          @Override
          public void startNotified(@NotNull ProcessEvent event) {
            try {
              final CharSequenceReader charSequenceReader = new CharSequenceReader(input);
              final BufferedWriter outputStreamWriter = new BufferedWriter(new OutputStreamWriter(handler.getProcessInput()));
              copy(charSequenceReader, outputStreamWriter);
              outputStreamWriter.close();
            }
            catch (IOException e) {
              logger.error(e);
            }
          }
        });
      }

      final ProgressIndicator progressIndicator = ProgressIndicatorProvider.getInstance().getProgressIndicator();
      final ProcessOutput output = handler.runProcessWithProgressIndicator(progressIndicator);

      lastCommand = command;

      if (output.isCancelled()) {
        // TODO: Vim will use whatever text has already been written to stdout
        // For whatever reason, we're not getting any here, so just throw an exception
        throw new ProcessCanceledException();
      }

      final Integer exitCode = handler.getExitCode();
      if (exitCode != null && exitCode != 0) {
        VimPlugin.showMessage("shell returned " + exitCode);
        VimPlugin.indicateError();
        return output.getStderr() + output.getStdout();
      }

      return output.getStdout();
    }, "IdeaVim filter command", true, editor.getProject());
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
