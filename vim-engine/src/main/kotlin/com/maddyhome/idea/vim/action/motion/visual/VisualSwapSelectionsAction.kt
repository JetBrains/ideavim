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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimScrollType
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.subMode

/**
 * @author vlan
 */
class VisualSwapSelectionsAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  // FIXME: 2019-03-05 Make it multicaret
  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return swapVisualSelections(editor)
  }
}

private fun swapVisualSelections(editor: VimEditor): Boolean {
  val lastSelectionType = editor.vimLastSelectionType ?: return false

  val lastVisualRange = injector.markGroup.getVisualSelectionMarks(editor) ?: return false
  val primaryCaret = editor.primaryCaret()
  editor.removeSecondaryCarets()
  val vimSelectionStart = primaryCaret.vimSelectionStart

  editor.vimLastSelectionType = SelectionType.fromSubMode(editor.subMode)
  injector.markGroup.setVisualSelectionMarks(editor, TextRange(vimSelectionStart, primaryCaret.offset.point))

  editor.subMode = lastSelectionType.toSubMode()
  primaryCaret.vimSetSelection(lastVisualRange.startOffset, lastVisualRange.endOffset, true)

  editor.scrollToCaret(VimScrollType.CENTER)

  return true
}
