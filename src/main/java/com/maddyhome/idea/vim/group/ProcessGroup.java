/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.api.VimProcessGroupBase;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.state.mode.Mode;
import com.maddyhome.idea.vim.state.VimStateMachine;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.InvalidCommandException;
import com.maddyhome.idea.vim.helper.UiHelper;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;

import static com.maddyhome.idea.vim.api.VimInjectorKt.globalOptions;
import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;


public class ProcessGroup extends VimProcessGroupBase {
  public String getLastCommand() {
    return lastCommand;
  }

  @Override
  public void startSearchCommand(@NotNull VimEditor editor, ExecutionContext context, int count, char leader) {
    if (((IjVimEditor) editor).getEditor().isOneLineMode()) // Don't allow searching in one line editors
    {
      return;
    }

    String initText = "";
    String label = String.valueOf(leader);

    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(((IjVimEditor) editor).getEditor(), ((DataContext) context.getContext()), label, initText, count);
  }

  @Override
  public @NotNull
  String endSearchCommand() {
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate(true);

    return panel.getText();
  }

  public void startExCommand(@NotNull VimEditor editor, ExecutionContext context, @NotNull Command cmd) {
    // Don't allow ex commands in one line editors
    if (editor.isOneLineMode()) return;

    String initText = getRange(((IjVimEditor) editor).getEditor(), cmd);
    injector.getMarkService().setVisualSelectionMarks(editor);
    VimStateMachine.Companion.getInstance(editor).setMode(Mode.CMD_LINE.INSTANCE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(((IjVimEditor) editor).getEditor(), ((IjEditorExecutionContext) context).getContext(), ":", initText, 1);
  }

  @Override
  public boolean processExKey(@NotNull VimEditor editor, @NotNull KeyStroke stroke) {
    // This will only get called if somehow the key focus ended up in the editor while the ex entry window
    // is open. So I'll put focus back in the editor and process the key.

    ExEntryPanel panel = ExEntryPanel.getInstance();
    if (panel.isActive()) {
      UiHelper.requestFocus(panel.getEntry());
      panel.handleKey(stroke);

      return true;
    } else {
      VimStateMachine.Companion.getInstance(editor).setMode(new Mode.NORMAL());
      KeyHandler.getInstance().reset(editor);
      return false;
    }
  }

  public boolean processExEntry(final @NotNull VimEditor editor, final @NotNull ExecutionContext context) {
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate(true);
    boolean res = true;
    try {
      VimStateMachine.Companion.getInstance(editor).setMode(new Mode.NORMAL());

      logger.debug("processing command");

      final String text = panel.getText();

      if (!panel.getLabel().equals(":")) {
        // Search is handled via Argument.Type.EX_STRING. Although ProcessExEntryAction is registered as the handler for
        // <CR> in both command and search modes, it's only invoked for command mode (see KeyHandler.handleCommandNode).
        // We should never be invoked for anything other than an actual ex command.
        throw new InvalidCommandException("Expected ':' command. Got '" + panel.getLabel() + "'", text);
      }

      if (logger.isDebugEnabled()) logger.debug("swing=" + SwingUtilities.isEventDispatchThread());

      VimInjectorKt.getInjector().getVimscriptExecutor().execute(text, editor, context, skipHistory(editor), true, CommandLineVimLContext.INSTANCE);
    } catch (ExException e) {
      VimPlugin.showMessage(e.getMessage());
      VimPlugin.indicateError();
      res = false;
    } catch (Exception bad) {
      ProcessGroup.logger.error(bad);
      VimPlugin.indicateError();
      res = false;
    }

    return res;
  }

  // commands executed from map command / macro should not be added to history
  private boolean skipHistory(VimEditor editor) {
    return VimStateMachine.Companion.getInstance(editor).getMappingState().isExecutingMap() || injector.getMacro().isExecutingMacro();
  }

  public void cancelExEntry(final @NotNull VimEditor editor, boolean resetCaret) {
    VimStateMachine.Companion.getInstance(editor).setMode(new Mode.NORMAL());
    KeyHandler.getInstance().reset(editor);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate(true, resetCaret);
  }

  @Override
  public void startFilterCommand(@NotNull VimEditor editor, ExecutionContext context, @NotNull Command cmd) {
    String initText = getRange(((IjVimEditor) editor).getEditor(), cmd) + "!";
    VimStateMachine.Companion.getInstance(editor).setMode(Mode.CMD_LINE.INSTANCE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(((IjVimEditor) editor).getEditor(), ((IjEditorExecutionContext) context).getContext(), ":", initText, 1);
  }

  private @NotNull
  String getRange(Editor editor, @NotNull Command cmd) {
    String initText = "";
    if (VimStateMachine.Companion.getInstance(new IjVimEditor(editor)).getMode() instanceof Mode.VISUAL) {
      initText = "'<,'>";
    } else if (cmd.getRawCount() > 0) {
      if (cmd.getCount() == 1) {
        initText = ".";
      } else {
        initText = ".,.+" + (cmd.getCount() - 1);
      }
    }

    return initText;
  }

  public @Nullable
  String executeCommand(@NotNull VimEditor editor, @NotNull String command, @Nullable CharSequence input, @Nullable String currentDirectoryPath)
    throws ExecutionException, ProcessCanceledException {

    // This is a much simplified version of how Vim does this. We're using stdin/stdout directly, while Vim will
    // redirect to temp files ('shellredir' and 'shelltemp') or use pipes. We don't support 'shellquote', because we're
    // not handling redirection, but we do use 'shellxquote' and 'shellxescape', because these have defaults that work
    // better with Windows. We also don't bother using ShellExecute for Windows commands beginning with `start`.
    // Finally, we're also not bothering with the crazy space and backslash handling of the 'shell' options content.
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {

      final String shell = globalOptions(injector).getShell();
      final String shellcmdflag = globalOptions(injector).getShellcmdflag();
      final String shellxescape = globalOptions(injector).getShellxescape();
      final String shellxquote = globalOptions(injector).getShellxquote();

      // For Win32. See :help 'shellxescape'
      final String escapedCommand = shellxquote.equals("(")
        ? doEscape(command, shellxescape, "^")
        : command;
      // Required for Win32+cmd.exe, defaults to "(". See :help 'shellxquote'
      final String quotedCommand = shellxquote.equals("(")
        ? "(" + escapedCommand + ")"
        : (shellxquote.equals("\"(")
        ? "\"(" + escapedCommand + ")\""
        : shellxquote + escapedCommand + shellxquote);

      final ArrayList<String> commands = new ArrayList<>();
      commands.add(shell);
      if (!shellcmdflag.isEmpty()) {
        // Note that Vim also does a simple whitespace split for multiple parameters
        commands.addAll(ParametersListUtil.parse(shellcmdflag));
      }
      commands.add(quotedCommand);

      if (logger.isDebugEnabled()) {
        logger.debug(String.format("shell=%s shellcmdflag=%s command=%s", shell, shellcmdflag, quotedCommand));
      }

      final GeneralCommandLine commandLine = new GeneralCommandLine(commands);
      if (currentDirectoryPath != null) {
        commandLine.setWorkDirectory(currentDirectoryPath);
      }
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
            } catch (IOException e) {
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
      }

      // Get stderr; stdout and strip colors, which are not handles properly.
      return (output.getStderr() + output.getStdout()).replaceAll("\u001B\\[[;\\d]*m", "");
    }, "IdeaVim - !" + command, true, ((IjVimEditor) editor).getEditor().getProject());
  }

  private String doEscape(String original, String charsToEscape, String escapeChar) {
    String result = original;
    for (char c : charsToEscape.toCharArray()) {
      result = result.replace("" + c, escapeChar + c);
    }
    return result;
  }

  // TODO: Java 10 has a transferTo method we could use instead
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
