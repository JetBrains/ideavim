/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputKeyStroke
import com.maddyhome.idea.vim.extension.abolish.CaseStyle
import com.maddyhome.idea.vim.extension.multiplecursors.selectRange
import com.maddyhome.idea.vim.extension.multiplecursors.selectedRange
import com.maddyhome.idea.vim.extension.multiplecursors.wordRange
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.inVisualMode

/**
 * The conversions of the vim-visual-multi menu, by key. Most of them come from vim-abolish.
 */
private val conversions: Map<Char, (String) -> String> = mapOf(
  'u' to String::lowercase,
  'U' to String::uppercase,
  'C' to { text -> text.lowercase().replaceFirstChar(Char::uppercaseChar) },
  't' to CaseStyle.TITLE::recase,
  'c' to CaseStyle.CAMEL::recase,
  'P' to CaseStyle.PASCAL::recase,
  's' to CaseStyle.SNAKE::recase,
  'S' to CaseStyle.UPPER_SNAKE::recase,
  '-' to CaseStyle.KEBAB::recase,
  '.' to CaseStyle.DOT::recase,
  ' ' to CaseStyle.SPACE::recase,
)

/**
 * Asks for a conversion, and converts the text of each region, or the word under each cursor
 */
internal class CaseConversionHandler : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val convert = conversions[inputKeyStroke(editor.ij).keyChar] ?: return
    val convertsRegions = editor.inVisualMode

    ApplicationManager.getApplication().runWriteAction {
      // From the last one, so the replacements don't move the ranges that are still to be converted
      for (caret in editor.ij.caretModel.allCarets.reversed()) {
        val range = (if (convertsRegions) caret.selectedRange else caret.wordRange()) ?: continue
        val converted = convert(editor.getText(range))
        editor.ij.document.replaceString(range.startOffset, range.endOffset, converted)
        updateCaret(caret, TextRange(range.startOffset, range.startOffset + converted.length), convertsRegions)
      }
    }
  }

  private fun updateCaret(caret: Caret, converted: TextRange, selects: Boolean) {
    if (selects) {
      caret.selectRange(converted)
    } else {
      caret.moveToOffset(converted.startOffset)
    }
  }
}
