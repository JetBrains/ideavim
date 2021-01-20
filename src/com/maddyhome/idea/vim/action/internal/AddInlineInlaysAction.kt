/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.action.internal

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.VimNlsSafe
import java.util.*
import kotlin.math.max

class AddInlineInlaysAction : AnAction() {
  companion object {
    private val random = Random()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val dataContext = e.dataContext
    val editor = getEditor(dataContext) ?: return
    val inlayModel = editor.inlayModel
    val currentVisualLine = editor.caretModel.primaryCaret.visualPosition.line
    var i = random.nextInt(10)
    val lineLength = EditorHelper.getLineLength(editor, EditorHelper.visualLineToLogicalLine(editor, currentVisualLine))
    while (i < lineLength) {
      val relatesToPrecedingText = random.nextInt(10) > 7

      @VimNlsSafe
      val text = "a".repeat(max(1, random.nextInt(7)))

      val offset = EditorHelper.visualPositionToOffset(editor, VisualPosition(currentVisualLine, i))
      // We don't need a custom renderer, just use the standard parameter hint renderer
      inlayModel.addInlineElement(offset, relatesToPrecedingText, HintRenderer(if (relatesToPrecedingText) ":$text" else "$text:"))
      // Every 20 chars +/- 5 chars
      i += 20 + (random.nextInt(10) - 5)
    }
  }

  private fun getEditor(dataContext: DataContext): Editor? {
    return CommonDataKeys.EDITOR.getData(dataContext)
  }
}
