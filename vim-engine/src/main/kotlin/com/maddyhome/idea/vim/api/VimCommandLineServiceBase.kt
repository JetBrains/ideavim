/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inVisualMode

abstract class VimCommandLineServiceBase : VimCommandLineService {
  override fun isCommandLineSupported(editor: VimEditor): Boolean {
    return !editor.isOneLineMode()
  }

  abstract fun createPanel(editor: VimEditor, context: ExecutionContext, label: String, initText: String): VimCommandLine

  private fun createCommandLinePrompt(editor: VimEditor, context: ExecutionContext, removeSelections: Boolean, label: String, initialText: String): VimCommandLine {
    if (!isCommandLineSupported(editor)) throw ExException("Command line is not allowed in one line editors")

    val currentMode = editor.mode

    if (removeSelections) {
      // Make sure the Visual selection marks are up to date before we use them.
      injector.markService.setVisualSelectionMarks(editor)
      // Note that we should remove selection and reset caret offset before we switch back to Normal mode and then enter
      // Command-line mode. However, some IdeaVim commands can handle multiple carets, including multiple carets with
      // selection (which might or might not be a block selection). Unfortunately, because we're just entering
      // Command-line mode, we don't know which command is going to be entered, so we can't remove selection here.
      // Therefore, we switch to Normal and then Command-line even though we might still have a Visual selection...
      // On the plus side, it means we still show selection while editing the command line, which Vim also does
      // (Normal then Command-line is not strictly necessary, but done for completeness and autocmd)
      // Caret selection is finally handled in Command.execute
      editor.mode = Mode.NORMAL()
    }
    editor.mode = Mode.CMD_LINE(currentMode)
    return createPanel(editor, context, label, initialText)
  }

  override fun createSearchPrompt(editor: VimEditor, context: ExecutionContext, label: String, initialText: String): VimCommandLine {
    return createCommandLinePrompt(editor, context, removeSelections = false, label, initialText)
  }

  override fun createCommandPrompt(editor: VimEditor, context: ExecutionContext, count0: Int, initialText: String): VimCommandLine {
    val rangeText = getRange(editor, count0)
    return createCommandLinePrompt(editor, context, removeSelections = true, label = ":", rangeText + initialText)
  }

  protected fun getRange(editor: VimEditor, count0: Int) = when {
    editor.inVisualMode -> "'<,'>"
    count0 == 1 -> "."
    count0 > 1 -> ".,.+" + (count0 - 1)
    else -> ""
  }
}
