package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchHelper
import com.maddyhome.idea.vim.helper.SearchHelper

@Service
class IjVimSearchHelper : VimSearchHelper {
  override fun findNextParagraph(editor: VimEditor, caret: VimCaret, count: Int, allowBlanks: Boolean): Int {
    return SearchHelper.findNextParagraph(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
      allowBlanks
    )
  }

  override fun findNextSentenceStart(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int {
    return SearchHelper.findNextSentenceStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count, countCurrent, requireAll
    )
  }

  override fun findSection(editor: VimEditor, caret: VimCaret, type: Char, dir: Int, count: Int): Int {
    return SearchHelper.findSection(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      type,
      dir,
      count,
    )
  }

  override fun findNextCamelEnd(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findNextCamelEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findNextSentenceEnd(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int {
    return SearchHelper.findNextSentenceEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
      countCurrent,
      requireAll,
    )
  }
}
