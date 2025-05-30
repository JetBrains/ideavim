/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.yank

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.state.mode.SelectionType

interface VimYankGroup {
  /**
   * This yanks the text moved over by the motion command argument.
   *
   * @param editor   The editor to yank from
   * @param context  The data context
   * @param argument The motion command argument
   * @return true if able to yank the text, false if not
   */
  fun yankMotion(
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun yankLine(editor: VimEditor, context: ExecutionContext, count: Int): Boolean

  fun yankRange(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange?,
    type: SelectionType,
    moveCursor: Boolean,
  ): Boolean
}
