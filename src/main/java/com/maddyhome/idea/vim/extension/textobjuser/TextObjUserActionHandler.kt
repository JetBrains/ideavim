/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Computes the [TextRange] of a user-defined text object for the operator/visual machinery.
 */
internal class TextObjUserActionHandler(
  private val patterns: List<String>,
  private val isInner: Boolean,
  private val regionType: SelectionType = SelectionType.CHARACTER_WISE,
) : TextObjectActionHandler() {

  // TextObjectVisualType has no blockwise value, so an operator over a blockwise object falls back to charwise.
  override val visualType: TextObjectVisualType
    get() = if (regionType == SelectionType.LINE_WISE) TextObjectVisualType.LINE_WISE else TextObjectVisualType.CHARACTER_WISE

  override fun getRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    count: Int,
    rawCount: Int,
  ): TextRange? {
    val caretOffset = caret.offset
    return when (patterns.size) {
      1 -> singlePatternRange(editor, caretOffset)
      2 -> pairPatternRange(editor, caretOffset)
      else -> null
    }
  }

  /**
   * Selects the whole match of a single pattern: the one under the cursor, or else the next one ahead of it.
   */
  private fun singlePatternRange(editor: VimEditor, caretOffset: Int): TextRange? {
    val matches = VimRegex(patterns[0]).findAll(editor)
    val match = matches.firstOrNull { caretOffset in it.range.startOffset until it.range.endOffset }
      ?: matches.firstOrNull { it.range.startOffset >= caretOffset }
      ?: return null
    return TextRange(match.range.startOffset, match.range.endOffset)
  }

  /**
   * Selects text delimited by a [header, footer] pair of patterns. "select-a" ([isInner] `false`) spans the delimiters,
   * from the header start to the footer end; "select-i" spans only the text between them, from the header end to the
   * footer start.
   */
  private fun pairPatternRange(editor: VimEditor, caretOffset: Int): TextRange? {
    // The header at or before the cursor (so the cursor may sit on the header, the enclosed text, or the footer),
    // falling back to the next header ahead of the cursor.
    val headers = VimRegex(patterns[0]).findAll(editor)
    val header = headers.lastOrNull { it.range.startOffset <= caretOffset }
      ?: headers.firstOrNull { it.range.startOffset >= caretOffset }
      ?: return null
    val footer = VimRegex(patterns[1]).findAll(editor)
      .firstOrNull { it.range.startOffset >= header.range.endOffset }
      ?: return null

    return if (isInner) {
      TextRange(header.range.endOffset, footer.range.startOffset)
    } else {
      TextRange(header.range.startOffset, footer.range.endOffset)
    }
  }
}
