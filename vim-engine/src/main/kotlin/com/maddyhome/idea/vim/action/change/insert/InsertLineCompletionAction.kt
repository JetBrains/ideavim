/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["<C-L>"], modes = [Mode.INSERT])
class InsertLineCompletionAction : VimActionHandler.SingleExecution() {

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (injector.vimState.ctrlXCompletionMode == com.maddyhome.idea.vim.state.mode.CtrlXCompletionMode.NONE) {
      return false
    }
    injector.vimState.ctrlXCompletionMode = com.maddyhome.idea.vim.state.mode.CtrlXCompletionMode.WHOLE_LINE

    val lookup = injector.lookupManager.getActiveLookup(editor)
    if (lookup != null) {
      lookup.down(editor.primaryCaret(), context)
      return true
    }

    val currentLine = editor.getLineText(editor.currentCaret().getLine()).trim()
    val line = getLineToMatch(editor)
    val lines = getMatchingLines(editor, line, currentLine)
    injector.lookupManager.showCustomLookup(editor, lines)
    return true
  }

  private fun getMatchingLines(
    editor: VimEditor,
    line: String,
    currentLine: String,
  ): List<String> =
    editor.text().split("\n").map { it.trim() }.filter { it.startsWith(line) && it != currentLine }.distinct()

  private fun getLineToMatch(editor: VimEditor): String {
    val lineStartOffset = editor.getLineStartOffset(editor.currentCaret().getLine())
    val line = editor.getText(lineStartOffset, editor.currentCaret().offset).trim()
    return line
  }

  override val type: Command.Type
    get() = Command.Type.INSERT

}
