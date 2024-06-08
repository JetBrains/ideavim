/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.undo

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.InsertSequence

public interface VimUndoRedo {
  public fun undo(editor: VimEditor, context: ExecutionContext): Boolean
  public fun redo(editor: VimEditor, context: ExecutionContext): Boolean

  public fun startInsertSequence(caret: VimCaret, startOffset: Int, startNanoTime: Long)
  public fun endInsertSequence(caret: VimCaret, endOffset: Int, endNanoTime: Long)
  public fun abandonCurrentInsertSequence(caret: VimCaret)
  public fun getInsertSequence(caret: VimCaret, nanoTime: Long): InsertSequence?
}
