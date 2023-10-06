/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchHelperBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import java.util.*

@Service
internal class IjVimSearchHelper : VimSearchHelperBase() {
  override fun findSection(editor: VimEditor, caret: ImmutableVimCaret, type: Char, dir: Int, count: Int): Int {
    return SearchHelper.findSection(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      type,
      dir,
      count,
    )
  }

  override fun findMethodEnd(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return SearchHelper.findMethodEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findMethodStart(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    return SearchHelper.findMethodStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findUnmatchedBlock(editor: VimEditor, caret: ImmutableVimCaret, type: Char, count: Int): Int {
    return SearchHelper.findUnmatchedBlock(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      type,
      count,
    )
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
    ignoreCase: Boolean
  ): List<TextRange> {
    return if (injector.globalIjOptions().useNewRegex) super.findAll(editor, pattern, startLine, endLine, ignoreCase)
    else SearchHelper.findAll(editor.ij, pattern, startLine, endLine, ignoreCase)
  }

  override fun findNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int {
    return SearchHelper.findNextCharacterOnLine(editor.ij, caret.ij, count, ch)
  }

  override fun findWordUnderCursor(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    dir: Int,
    isOuter: Boolean,
    isBig: Boolean,
    hasSelection: Boolean,
  ): TextRange {
    return SearchHelper.findWordUnderCursor(editor.ij, caret.ij, count, dir, isOuter, isBig, hasSelection)
  }

  override fun findBlockTagRange(editor: VimEditor, caret: ImmutableVimCaret, count: Int, isOuter: Boolean): TextRange? {
    return SearchHelper.findBlockTagRange(editor.ij, caret.ij, count, isOuter)
  }

  override fun findBlockRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
    return SearchHelper.findBlockRange(editor.ij, caret.ij, type, count, isOuter)
  }
}
