/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.change

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService

abstract class ChangeInInsertSequenceAction : ChangeEditorActionHandler.ForEachCaret() {
  final override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // We start an insert sequence before entering the insert mode to unify future world deletion with future typing into a single undo step
    val undo = injector.undo
    when (undo) {
      is VimKeyBasedUndoService -> undo.setInsertNonMergeUndoKey()
      is VimTimestampBasedUndoService -> {
        undo.startInsertSequence(caret, caret.offset, System.nanoTime())
      }
    }
    val result = executeInInsertSequence(editor, caret, context, argument, operatorArguments)
    if (!result) (injector.undo as? VimTimestampBasedUndoService)?.abandonCurrentInsertSequence(caret)
    return result
  }

  abstract fun executeInInsertSequence(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean
}