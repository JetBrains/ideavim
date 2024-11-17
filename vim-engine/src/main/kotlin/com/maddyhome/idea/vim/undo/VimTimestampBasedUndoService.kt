/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.undo

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.common.InsertSequence

/**
 * IJ-like undo service interface
 * TBH I don't really understand how the proper undo API for IJ should look like.
 * These methods are definitely not enough, but it works because commands are managed and added to history automatically.
 * TODO [IdeaVim developer] it would be cool to make commands part of this service, to make undo subsystem more clear
 */
interface VimTimestampBasedUndoService : VimUndoRedo {
  fun startInsertSequence(caret: VimCaret, startOffset: Int, startNanoTime: Long)
  fun endInsertSequence(caret: VimCaret, endOffset: Int, endNanoTime: Long)
  fun abandonCurrentInsertSequence(caret: VimCaret)
  fun getInsertSequence(caret: VimCaret, nanoTime: Long): InsertSequence?
}