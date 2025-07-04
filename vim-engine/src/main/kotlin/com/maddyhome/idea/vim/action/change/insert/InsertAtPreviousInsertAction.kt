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
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.handler.Motion

@CommandOrMotion(keys = ["gi"], modes = [Mode.NORMAL])
class InsertAtPreviousInsertAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    insertAtPreviousInsert(editor, context)
    return true
  }
}

/**
 * Begin insert at the location of the previous insert
 *
 * @param editor The editor to insert into
 */
// todo make it multicaret
private fun insertAtPreviousInsert(editor: VimEditor, context: ExecutionContext) {
  editor.removeSecondaryCarets()
  val caret = editor.primaryCaret()
  val motion = injector.motion.moveCaretToMark(editor.primaryCaret(), VimMarkService.INSERT_EXIT_MARK, false)
  if (motion is Motion.AbsoluteOffset) {
    caret.moveToOffset(motion.offset)
  }
  injector.changeGroup.insertBeforeCaret(editor, context)
}
