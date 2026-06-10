/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.helper.EngineStringHelper

/**
 * Colors the caret-notation (`^M`, `^[`, …) and `<hex>` tokens in the control-chars editor so
 * control characters stand out from ordinary text, and keeps them coloured as the user edits.
 */
internal object ControlCharsEditorHighlighter {
  private val ATTRIBUTES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "IDEAVIM_CONTROL_CHARACTER",
    DefaultLanguageHighlighterColors.KEYWORD,
  )

  /** Marks the range highlighters we own, so we only ever remove our own. */
  private val OURS: Key<Boolean> = Key.create("ideavim.controlChars.highlighter")

  fun install(editor: Editor) {
    highlight(editor)
    // Re-highlight on every edit; cleaned up automatically when the editor is released.
    val parent = (editor as? EditorImpl)?.disposable ?: return
    editor.document.addDocumentListener(
      object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) = highlight(editor)
      },
      parent,
    )
  }

  private fun highlight(editor: Editor) {
    val markupModel = editor.markupModel
    markupModel.allHighlighters
      .filter { it.getUserData(OURS) == true }
      .forEach { markupModel.removeHighlighter(it) }

    for (range in EngineStringHelper.controlTokenRanges(editor.document.text)) {
      markupModel.addRangeHighlighter(
        ATTRIBUTES,
        range.first,
        range.last + 1,
        HighlighterLayer.ADDITIONAL_SYNTAX,
        HighlighterTargetArea.EXACT_RANGE,
      ).putUserData(OURS, true)
    }
  }
}
