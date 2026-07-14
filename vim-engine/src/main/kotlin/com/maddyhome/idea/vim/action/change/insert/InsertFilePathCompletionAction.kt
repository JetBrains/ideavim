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
import com.maddyhome.idea.vim.state.mode.CtrlXCompletionMode

@CommandOrMotion(keys = ["<C-F>"], modes = [Mode.INSERT])
class InsertFilePathCompletionAction : VimActionHandler.SingleExecution() {

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (injector.vimState.ctrlXCompletionMode == CtrlXCompletionMode.NONE) {
      return false
    }
    injector.vimState.ctrlXCompletionMode = CtrlXCompletionMode.FILE_PATH

    val lookup = injector.lookupManager.getActiveLookup(editor)
    if (lookup != null) {
      lookup.down(editor.primaryCaret(), context)
      return true
    }

    val line = getLineToMatch(editor)
    val lines = getMatchingLines(context, line)
    injector.lookupManager.showCustomLookup(editor, lines, getReplacePrefix(editor))
    return true
  }

  private fun getMatchingLines(
    context: ExecutionContext,
    line: String,
  ): List<String> {
    return injector.file.listFilesForCompletion(line, context)
  }

  private fun getLineToMatch(editor: VimEditor): String {
    val lineStartOffset = editor.getLineStartOffset(editor.currentCaret().getLine())
    val line = editor.getText(lineStartOffset, editor.currentCaret().offset).trim()
    return line
  }

  private fun getReplacePrefix(editor: VimEditor): String {
    val lineStartOffset = editor.getLineStartOffset(editor.currentCaret().getLine())
    return editor.getText(lineStartOffset, editor.currentCaret().offset).trimStart()
  }

  override val type: Command.Type
    get() = Command.Type.INSERT

}
