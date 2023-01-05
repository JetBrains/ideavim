/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.visual

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.group.visual.vimSetSelection
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

  injector.scroll.scrollCaretIntoView(editor)

  return true
}
