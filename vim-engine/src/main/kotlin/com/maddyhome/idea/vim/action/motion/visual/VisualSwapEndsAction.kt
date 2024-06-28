/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.state.mode.inBlockSelection

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["o"], modes = [Mode.VISUAL])
class VisualSwapEndsAction : VimActionHandler.ForEachCaret() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean = swapVisualEnds(caret)
}

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["O"], modes = [Mode.VISUAL])
class VisualSwapEndsBlockAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.inBlockSelection) {
      return swapVisualEndsBigO(editor)
    }

    var ret = true
    for (caret in editor.carets()) {
      ret = ret && swapVisualEnds(caret)
    }
    return ret
  }
}

private fun swapVisualEnds(caret: VimCaret): Boolean {
  val vimSelectionStart = caret.vimSelectionStart
  caret.vimSelectionStart = caret.offset

  caret.moveToOffset(vimSelectionStart)

  return true
}

private fun swapVisualEndsBigO(editor: VimEditor): Boolean {
  val caret = editor.primaryCaret()
  val anotherSideCaret = editor.nativeCarets().let { if (it.first() == caret) it.last() else it.first() }

  val adj = injector.visualMotionGroup.selectionAdj

  if (caret.offset == caret.selectionStart) {
    caret.vimSelectionStart = anotherSideCaret.selectionStart
    caret.moveToOffset(caret.selectionEnd - adj)
  } else {
    caret.vimSelectionStart = anotherSideCaret.selectionEnd - adj
    caret.moveToOffset(caret.selectionStart)
  }

  return true
}
