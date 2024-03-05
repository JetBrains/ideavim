/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchHelperBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import it.unimi.dsi.fastutil.ints.IntComparator
import it.unimi.dsi.fastutil.ints.IntComparators
import java.util.*

@Service
internal class IjVimSearchHelper : VimSearchHelperBase() {
  companion object {
    private val logger = Logger.getInstance(IjVimSearchHelper::class.java.name)
  }

  override fun findMethodEnd(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    // TODO add it to PsiService
    return PsiHelper.findMethodEnd(editor.ij, caret.ij.offset, count)
  }

  override fun findMethodStart(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    // TODO add it to PsiService
    return PsiHelper.findMethodStart(editor.ij, caret.ij.offset, count)
  }

  override fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange? {
    return if (injector.globalIjOptions().useNewRegex) super.findPattern(editor, pattern, startOffset, count, searchOptions)
    else SearchHelper.findPattern(editor.ij, pattern, startOffset, count, searchOptions)
  }

  override fun findAll(
    editor: VimEditor,
    pattern: String,
    startLine: Int,
    endLine: Int,
    ignoreCase: Boolean,
  ): List<TextRange> {
    return if (injector.globalIjOptions().useNewRegex) super.findAll(editor, pattern, startLine, endLine, ignoreCase)
    else SearchHelper.findAll(editor.ij, pattern, startLine, endLine, ignoreCase)
  }

  override fun findMisspelledWord(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    val startOffset: Int
    val endOffset: Int
    val skipCount: Int
    val offsetOrdering: IntComparator

    if (count < 0) {
      startOffset = 0
      endOffset = caret.offset.point - 1
      skipCount = -count - 1
      offsetOrdering = IntComparators.OPPOSITE_COMPARATOR
    }
    else {
      startOffset = caret.offset.point + 1
      endOffset = editor.ij.document.textLength
      skipCount = count - 1
      offsetOrdering = IntComparators.NATURAL_COMPARATOR
    }

    // TODO add it to PsiService
    return SearchHelper.findMisspelledWords(editor.ij, startOffset, endOffset, skipCount, offsetOrdering)
  }
}
