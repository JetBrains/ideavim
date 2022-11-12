/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class InsertAtPreviousInsertAction : ChangeEditorActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_MULTIKEY_UNDO)

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
fun insertAtPreviousInsert(editor: VimEditor, context: ExecutionContext) {
  editor.removeSecondaryCarets()
  val caret = editor.primaryCaret()
  val offset = injector.motion.moveCaretToMark(editor, editor.primaryCaret(), '^', false)
  if (offset != -1) {
    caret.moveToOffset(offset)
  }
  injector.changeGroup.insertBeforeCursor(editor, context)
}
