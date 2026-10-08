/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.multiplecursors.makePrimary
import com.maddyhome.idea.vim.extension.multiplecursors.occurrenceSearch
import com.maddyhome.idea.vim.extension.multiplecursors.selectedRange
import com.maddyhome.idea.vim.newapi.ij

/**
 * Makes the next or the previous region the current one, without adding any. Wraps around.
 *
 * The current region is the primary caret. The search continues from it, in the same direction.
 */
internal class GotoRegionHandler(private val direction: Direction) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    repeat(operatorArguments.count1) {
      gotoRegion(editor.ij)
    }
  }

  private fun gotoRegion(editor: Editor) {
    val carets = editor.caretModel.allCarets
    if (carets.size < 2) return

    val current = carets.indexOf(editor.caretModel.primaryCaret)
    val target = carets[(current + direction.toInt()).mod(carets.size)]
    val region = editor.makePrimary(target).selectedRange
    editor.occurrenceSearch = editor.occurrenceSearch?.copy(lastOccurrence = region, direction = direction)
  }
}
