/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.action.motion.visual

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.inBlockSubMode

/**
 * @author vlan
 */
class VisualSwapEndsAction : VimActionHandler.ForEachCaret() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments
  ): Boolean = swapVisualEnds(caret)
}

/**
 * @author vlan
 */
class VisualSwapEndsBlockAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    if (editor.inBlockSubMode) {
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
  caret.vimSelectionStart = caret.offset.point

  caret.moveToOffset(vimSelectionStart)

  return true
}

private fun swapVisualEndsBigO(editor: VimEditor): Boolean {
  val caret = editor.primaryCaret()
  val anotherSideCaret = editor.nativeCarets().let { if (it.first() == caret) it.last() else it.first() }

  val adj = injector.visualMotionGroup.selectionAdj

  if (caret.offset.point == caret.selectionStart) {
    caret.vimSelectionStart = anotherSideCaret.selectionStart
    caret.moveToOffset(caret.selectionEnd - adj)
  } else {
    caret.vimSelectionStart = anotherSideCaret.selectionEnd - adj
    caret.moveToOffset(caret.selectionStart)
  }

  return true
}
