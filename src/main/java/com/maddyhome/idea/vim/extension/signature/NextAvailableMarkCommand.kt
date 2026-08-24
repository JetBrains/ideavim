/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.signature

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler

internal class NextAvailableMarkCommand : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val localMarks =
      injector.markService.getAllLocalMarks(editor.primaryCaret()).filter { it.key in VimMarkService.LOWERCASE_MARKS }
    val availableMarks = VimMarkService.LOWERCASE_MARKS.filter { mark -> localMarks.none { it.key == mark } }
    if (availableMarks.isEmpty()) {
      injector.messages.indicateError()
      return
    }
    val nextMark = availableMarks.first()
    injector.markService.setMark(editor.primaryCaret(), nextMark, editor.primaryCaret().offset)
  }
}