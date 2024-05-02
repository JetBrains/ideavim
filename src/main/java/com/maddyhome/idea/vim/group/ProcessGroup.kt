/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.text.CharSequenceReader
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimProcessGroupBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.helper.requestFocus
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.returnTo
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import javax.swing.KeyStroke

public class ProcessGroup : VimProcessGroupBase() {
  override var lastCommand: String? = null
    private set
  override var isCommandProcessing: Boolean = false
  override var modeBeforeCommandProcessing: Mode? = null

  public override fun startExEntry(
    editor: VimEditor,
    context: ExecutionContext,
    command: Command,
    label: String,
    initialCommandText: String,
  ) {
    // Don't allow ex commands in one line editors
    if (editor.isOneLineMode()) return

    val currentMode = editor.vimStateMachine.mode
    check(currentMode is ReturnableFromCmd) {
      "Cannot enable cmd mode from current mode $currentMode"
    }

    isCommandProcessing = true
    modeBeforeCommandProcessing = currentMode

    // Make sure the Visual selection marks are up to date before we use them.
    injector.markService.setVisualSelectionMarks(editor)

    val rangeText = getRange(editor, command)

    // Note that we should remove selection and reset caret offset before we switch back to Normal mode and then enter
    // Command-line mode. However, some IdeaVim commands can handle multiple carets, including multiple carets with
    // selection (which might or might not be a block selection). Unfortunately, because we're just entering
    // Command-line mode, we don't know which command is going to be entered, so we can't remove selection here.
    // Therefore, we switch to Normal and then Command-line even though we might still have a Visual selection...
    // On the plus side, it means we still show selection while editing the command line, which Vim also does
    // (Normal then Command-line is not strictly necessary, but done for completeness and autocmd)
    // Caret selection is finally handled in Command.execute
    editor.mode = Mode.NORMAL()
    editor.mode = Mode.CMD_LINE(currentMode)

    injector.commandLine.create(editor, context, ":", rangeText + initialCommandText, 1)
  }

  public override fun processExKey(editor: VimEditor, stroke: KeyStroke, processResultBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean {
    // This will only get called if somehow the key focus ended up in the editor while the ex entry window
    // is open. So I'll put focus back in the editor and process the key.

    val panel = ExEntryPanel.getInstance()
    if (panel.isActive) {
      processResultBuilder.addExecutionStep { _, _, _ ->
        requestFocus(panel.entry)
        panel.handleKey(stroke)
      }
      return true
    } else {
      processResultBuilder.addExecutionStep { _, lambdaEditor, _ ->
        lambdaEditor.mode = Mode.NORMAL()
        getInstance().reset(lambdaEditor)
      }
      return false
    }
  }

  public override fun cancelExEntry(editor: VimEditor, resetCaret: Boolean) {
    // If 'cpoptions' contains 'x', then Escape should execute the command line. This is the default for Vi but not Vim.
    // IdeaVim does not (currently?) support 'cpoptions', so sticks with Vim's default behaviour. Escape cancels.
    editor.mode = editor.mode.returnTo()
    getInstance().reset(editor)
    val panel = ExEntryPanel.getInstance()
    panel.deactivate(true, resetCaret)
  }

  private fun getRange(editor: VimEditor, cmd: Command): String {
    var initText = ""
    if (editor.inVisualMode) {
      initText = "'<,'>"
    } else if (cmd.rawCount > 0) {
      initText = if (cmd.count == 1) {
        "."
      } else {
        ".,.+" + (cmd.count - 1)
      }
    }

    return initText
  }

  @Throws(ExecutionException::class, ProcessCanceledException::class)
  public override fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?
  ): String? {
    // This is a much simplified version of how Vim does this. We're using stdin/stdout directly, while Vim will
    // redirect to temp files ('shellredir' and 'shelltemp') or use pipes. We don't support 'shellquote', because we're
    // not handling redirection, but we do use 'shellxquote' and 'shellxescape', because these have defaults that work
    // better with Windows. We also don't bother using ShellExecute for Windows commands beginning with `start`.
    // Finally, we're also not bothering with the crazy space and backslash handling of the 'shell' options content.

    return ProgressManager.getInstance().runProcessWithProgressSynchronously<String, ExecutionException>(
      {
        val shell = injector.globalOptions().shell
        val shellcmdflag = injector.globalOptions().shellcmdflag
        val shellxescape = injector.globalOptions().shellxescape
        val shellxquote = injector.globalOptions().shellxquote

        // For Win32. See :help 'shellxescape'
        val escapedCommand = if (shellxquote == "(") doEscape(command, shellxescape, "^")
        else command
        // Required for Win32+cmd.exe, defaults to "(". See :help 'shellxquote'
        val quotedCommand = if (shellxquote == "(") "($escapedCommand)"
        else (if (shellxquote == "\"(") "\"($escapedCommand)\""
        else shellxquote + escapedCommand + shellxquote)

        val commands = ArrayList<String>()
        commands.add(shell)
        if (shellcmdflag.isNotEmpty()) {
          // Note that Vim also does a simple whitespace split for multiple parameters
          commands.addAll(ParametersListUtil.parse(shellcmdflag))
        }
        commands.add(quotedCommand)

        if (logger.isDebugEnabled) {
          logger.debug(String.format("shell=%s shellcmdflag=%s command=%s", shell, shellcmdflag, quotedCommand))
        }

        val commandLine = GeneralCommandLine(commands)
        if (currentDirectoryPath != null) {
          commandLine.setWorkDirectory(currentDirectoryPath)
        }
        val handler = CapturingProcessHandler(commandLine)
        if (input != null) {
          handler.addProcessListener(object : ProcessAdapter() {
            override fun startNotified(event: ProcessEvent) {
              try {
                val charSequenceReader = CharSequenceReader(input)
                val outputStreamWriter = BufferedWriter(OutputStreamWriter(handler.processInput))
                copy(charSequenceReader, outputStreamWriter)
                outputStreamWriter.close()
              } catch (e: IOException) {
                logger.error(e)
              }
            }
          })
        }

        val progressIndicator = ProgressIndicatorProvider.getInstance().progressIndicator
        val output = handler.runProcessWithProgressIndicator(progressIndicator)

        lastCommand = command

        if (output.isCancelled) {
          // TODO: Vim will use whatever text has already been written to stdout
          // For whatever reason, we're not getting any here, so just throw an exception
          throw ProcessCanceledException()
        }

        val exitCode = handler.exitCode
        if (exitCode != null && exitCode != 0) {
          VimPlugin.showMessage("shell returned $exitCode")
          VimPlugin.indicateError()
        }
        (output.stderr + output.stdout).replace("\u001B\\[[;\\d]*m".toRegex(), "")
      }, "IdeaVim - !$command", true, editor.ij.project
    )
  }

  @Suppress("SameParameterValue")
  private fun doEscape(original: String, charsToEscape: String, escapeChar: String): String {
    var result = original
    for (c in charsToEscape.toCharArray()) {
      result = result.replace("" + c, escapeChar + c)
    }
    return result
  }

  // TODO: Java 10 has a transferTo method we could use instead
  @Throws(IOException::class)
  private fun copy(from: Reader, to: Writer) {
    val buf = CharArray(2048)
    var cnt: Int
    while ((from.read(buf).also { cnt = it }) != -1) {
      to.write(buf, 0, cnt)
    }
  }

  public companion object {
    private val logger = logger<ProcessGroup>()
  }
}
