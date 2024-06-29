/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.returnTo
import javax.swing.KeyStroke

abstract class VimProcessGroupBase : VimProcessGroup {
  override var lastCommand: String? = null
  override var isCommandProcessing: Boolean = false
  override var modeBeforeCommandProcessing: Mode? = null

  override fun startExEntry(editor: VimEditor, context: ExecutionContext, command: Command, label: String, initialCommandText: String) {
    // Don't allow ex commands in one line editors
    if (editor.isOneLineMode()) return

    val currentMode = editor.mode
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

    injector.commandLine.create(editor, context, ":", rangeText + initialCommandText)
  }

  override fun processExKey(editor: VimEditor, stroke: KeyStroke, processResultBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean {
    // This will only get called if somehow the key focus ended up in the editor while the ex entry window
    // is open. So I'll put focus back in the editor and process the key.
    // FIXME comment above is not true. This method is called all the time. Is there a way to make it work like in the comment above?
    // TODO maybe something like `Propagate.CONTINUE` will help

    val panel = injector.commandLine.getActiveCommandLine()
    if (panel != null) {
      processResultBuilder.addExecutionStep { _, _, _ ->
        panel.focus()
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

  override fun cancelExEntry(editor: VimEditor, resetCaret: Boolean) {
    // If 'cpoptions' contains 'x', then Escape should execute the command line. This is the default for Vi but not Vim.
    // IdeaVim does not (currently?) support 'cpoptions', so sticks with Vim's default behaviour. Escape cancels.
    editor.mode = editor.mode.returnTo()
    getInstance().reset(editor)
    injector.commandLine.getActiveCommandLine()?.deactivate(refocusOwningEditor = true, resetCaret)
  }

  private fun getRange(editor: VimEditor, cmd: Command) = when {
    editor.inVisualMode -> "'<,'>"
    cmd.rawCount == 1 -> "."
    cmd.rawCount > 1 -> ".,.+" + (cmd.count - 1)
    else -> ""
  }
}
