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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setVisualSelectionMarks
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["gv"], modes = [Mode.VISUAL])
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
  val mode = editor.mode
  check(mode is com.maddyhome.idea.vim.state.mode.Mode.VISUAL)

  val lastSelectionType = editor.vimLastSelectionType ?: return false

  val primaryCaret = editor.primaryCaret()
  val lastVisualRange = injector.markService.getVisualSelectionMarks(primaryCaret) ?: return false
  editor.removeSecondaryCarets()
  val vimSelectionStart = primaryCaret.vimSelectionStart

  editor.vimLastSelectionType = mode.selectionType
  injector.markService.setVisualSelectionMarks(primaryCaret, TextRange(vimSelectionStart, primaryCaret.offset))

  editor.mode = mode.copy(selectionType = lastSelectionType)
  primaryCaret.vimSetSelection(lastVisualRange.startOffset, lastVisualRange.endOffset, true)

  injector.scroll.scrollCaretIntoView(editor)

  return true
}
