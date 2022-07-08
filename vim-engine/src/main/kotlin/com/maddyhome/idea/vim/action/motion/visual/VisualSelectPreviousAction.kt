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
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.VimScrollType
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.vimStateMachine

/**
 * @author vlan
 */
class VisualSelectPreviousAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean { // FIXME: 2019-03-05 Make it multicaret
    return selectPreviousVisualMode(editor)
  }
}

private fun selectPreviousVisualMode(editor: VimEditor): Boolean {
  val lastSelectionType = editor.vimLastSelectionType ?: return false
  val visualMarks = injector.markGroup.getVisualSelectionMarks(editor) ?: return false

  editor.removeSecondaryCarets()

  editor.vimStateMachine.pushModes(VimStateMachine.Mode.VISUAL, lastSelectionType.toSubMode())

  val primaryCaret = editor.primaryCaret()
  primaryCaret.vimSetSelection(visualMarks.startOffset, visualMarks.endOffset - 1, true)

  editor.scrollToCaret(VimScrollType.CENTER)

  return true
}
