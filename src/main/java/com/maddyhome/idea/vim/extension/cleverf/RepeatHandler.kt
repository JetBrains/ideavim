/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler

/** Handles `<Plug>(clever-f-repeat-forward)` and `<Plug>(clever-f-repeat-back)` */
internal class RepeatHandler(
  private val back: Boolean,
  private val session: CleverFSession,
  private val executor: SearchExecutor,
) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val state = session.stateFor(editor.mode) ?: return
    executor.execute(editor, state.repeatSearch(back), operatorArguments.count1)
  }
}
