/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Font

/**
 * Highlights the characters the current search can jump to, like clever-f's `g:clever_f_mark_char`.
 * Only one set of candidates is shown at a time.
 */
internal class CandidateHighlighter {
  private var editor: Editor? = null
  private val highlighters = mutableListOf<RangeHighlighter>()

  fun show(editor: Editor, offsets: List<Int>) {
    clear()
    this.editor = editor
    offsets.mapTo(highlighters) { offset ->
      editor.markupModel.addRangeHighlighter(
        offset,
        offset + 1,
        HighlighterLayer.SELECTION,
        CANDIDATE_ATTRIBUTES,
        HighlighterTargetArea.EXACT_RANGE,
      )
    }
  }

  fun clear() {
    val markupModel = editor?.takeUnless { it.isDisposed }?.markupModel
    if (markupModel != null) highlighters.forEach(markupModel::removeHighlighter)
    highlighters.clear()
    editor = null
  }

  private companion object {
    /** clever-f's `CleverFDefaultLabel`: red, bold and underlined text */
    val CANDIDATE_ATTRIBUTES = TextAttributes(JBColor.RED, null, JBColor.RED, EffectType.LINE_UNDERSCORE, Font.BOLD)
  }
}
