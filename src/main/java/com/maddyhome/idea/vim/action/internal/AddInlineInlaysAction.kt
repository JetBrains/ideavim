/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.internal

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.newapi.vim
import java.util.*
import kotlin.math.max

internal class AddInlineInlaysAction : AnAction() {
  companion object {
    private val random = Random()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val dataContext = e.dataContext
    val editor = getEditor(dataContext) ?: return
    val vimEditor = editor.vim
    val inlayModel = editor.inlayModel
    val currentVisualLine = editor.caretModel.primaryCaret.visualPosition.line
    var i = random.nextInt(10)
    val lineLength = vimEditor.lineLength(vimEditor.visualLineToBufferLine(currentVisualLine))
    while (i < lineLength) {
      val relatesToPrecedingText = random.nextInt(10) > 7

      @VimNlsSafe
      val text = "a".repeat(max(1, random.nextInt(7)))

      val offset = editor.visualPositionToOffset(VisualPosition(currentVisualLine, i))
      // We don't need a custom renderer, just use the standard parameter hint renderer
      inlayModel.addInlineElement(
        offset,
        relatesToPrecedingText,
        HintRenderer(if (relatesToPrecedingText) ":$text" else "$text:"),
      )
      // Every 20 chars +/- 5 chars
      i += 20 + (random.nextInt(10) - 5)
    }
  }

  private fun getEditor(dataContext: DataContext): Editor? {
    return CommonDataKeys.EDITOR.getData(dataContext)
  }
}
