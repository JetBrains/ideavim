/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.vim.api.models.Color
import com.intellij.vim.api.models.HighlightId
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import java.awt.Font
import java.awt.Color as AwtColor

internal class IjHighlightId(
  internal var ijHighlighter: RangeHighlighter
): HighlightId

private fun Color.toAwtColor(): AwtColor = AwtColor(r, g, b, a)

class IjVimHighlightingService : VimHighlightingService {
  override fun addHighlighter(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
    backgroundColor: Color?,
    foregroundColor: Color?,
  ): HighlightId {
    val ijEditor = editor.ij

    val attributes = TextAttributes(
      foregroundColor?.toAwtColor(),
      backgroundColor?.toAwtColor(),
      ijEditor.colorsScheme.getColor(EditorColors.CARET_COLOR),
      EffectType.SEARCH_MATCH,
      Font.PLAIN,
    )

    val iJHighlighter = ijEditor.markupModel.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.SELECTION,
      attributes,
      HighlighterTargetArea.EXACT_RANGE,
    )

    val highlighter = IjHighlightId(iJHighlighter)
    return highlighter
  }

  override fun removeHighlighter(editor: VimEditor, highlightId: HighlightId) {
    val ijEditor = editor.ij
    ijEditor.markupModel.removeHighlighter((highlightId as IjHighlightId).ijHighlighter)
  }
}