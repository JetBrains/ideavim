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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler

internal class RemoveLineMarkCommand : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val marks = injector.markService.getAllLocalMarks(editor.primaryCaret())
    val currentLine = editor.primaryCaret().getLine()
    val toDelete = marks.filter { it.line == currentLine }
    toDelete.forEach { injector.markService.removeMark(editor, it.key) }
  }
}
