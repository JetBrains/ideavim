/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType

/**
 * `I` and `A` of characterwise Visual mode with multiple cursors: vim-multiple-cursors inserts before or appends after
 * each selection, not at the start or the end of its line as in Vim. Otherwise, the [keys] keep their Vim meaning.
 *
 * vim-multiple-cursors does it for the occurrences selected by itself, even if it is only one.
 */
internal class VisualInsertHandler(keys: String, command: String) : ExtensionHandler {

  private val insert = InsertHandler(command)
  private val native = NativeKeysHandler(injector.parser.parseKeys(keys))

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val inMultipleCursors = editor.mode.selectionType == SelectionType.CHARACTER_WISE && hasMultipleCursors(editor.ij)
    val handler = if (inMultipleCursors) insert else native
    handler.execute(editor, context, operatorArguments)
  }

  private fun hasMultipleCursors(editor: Editor): Boolean =
    editor.caretModel.caretCount > 1 ||
      editor.occurrenceSearch?.lastOccurrence == editor.caretModel.primaryCaret.selectedRange
}
