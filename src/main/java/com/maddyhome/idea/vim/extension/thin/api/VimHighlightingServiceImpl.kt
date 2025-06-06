/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.vim.api.Color
import com.intellij.vim.api.Highlighter
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.thinapi.VimHighlightingService
import com.maddyhome.idea.vim.thinapi.toAwtColor
import java.awt.Font

internal class IjHighlighter(
  internal var ijHighlighter: RangeHighlighter
): Highlighter


class VimHighlightingServiceImpl : VimHighlightingService {
  override fun addHighlighter(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
    backgroundColor: Color?,
    foregroundColor: Color?,
  ): Highlighter {
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

    val highlighter = IjHighlighter(iJHighlighter)
    return highlighter
  }

  override fun removeHighlighter(editor: VimEditor, highlighter: Highlighter) {
    val ijEditor = editor.ij
    ijEditor.markupModel.removeHighlighter((highlighter as IjHighlighter).ijHighlighter)
  }
}