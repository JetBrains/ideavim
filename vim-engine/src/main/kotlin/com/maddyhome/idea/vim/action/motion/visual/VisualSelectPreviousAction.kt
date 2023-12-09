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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.vimStateMachine

/**
 * @author vlan
 */
@CommandOrMotion(keys = ["gv"], modes = [Mode.NORMAL])
public class VisualSelectPreviousAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val selectionType = editor.primaryCaret().lastSelectionInfo.selectionType
    val caretToSelectionInfo = editor.carets()
      .map { it to it.lastSelectionInfo }

    if (caretToSelectionInfo.any { it.second.start == null || it.second.end == null }) return false

    editor.vimStateMachine.mode = com.maddyhome.idea.vim.state.mode.Mode.VISUAL(selectionType)

    for ((caret, selectionInfo) in caretToSelectionInfo) {
      val startOffset = editor.bufferPositionToOffset(selectionInfo.start!!)
      val endOffset = editor.bufferPositionToOffset(selectionInfo.end!!)
      caret.vimSetSelection(startOffset, endOffset, true)
    }

    injector.scroll.scrollCaretIntoView(editor)

    return true
  }
}
