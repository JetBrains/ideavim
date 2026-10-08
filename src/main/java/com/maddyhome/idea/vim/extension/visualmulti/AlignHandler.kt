/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.inVisualMode

/**
 * Inserts spaces before the cursors, so that they are all in the column of the rightmost one. The regions become
 * cursors first.
 */
internal class AlignHandler : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    if (editor.inVisualMode) {
      collapseRegionsToCursors(editor)
    }

    val carets = editor.ij.caretModel.allCarets
    val alignedColumn = carets.maxOf { it.logicalPosition.column }
    ApplicationManager.getApplication().runWriteAction {
      // From the last one, so the inserted spaces don't move the cursors that are still to be aligned
      for (caret in carets.reversed()) {
        val offset = caret.offset
        val spaces = alignedColumn - caret.logicalPosition.column
        editor.ij.document.insertString(offset, " ".repeat(spaces))
        caret.moveToOffset(offset + spaces)
      }
    }
  }
}
