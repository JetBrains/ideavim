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

  override fun findNextCamelStart(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findNextCamelStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findMethodEnd(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findMethodEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findMethodStart(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findMethodStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findUnmatchedBlock(editor: VimEditor, caret: VimCaret, type: Char, count: Int): Int {
    return SearchHelper.findUnmatchedBlock(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      type,
      count,
    )
  }

  override fun findNextWordEnd(editor: VimEditor, caret: VimCaret, count: Int, bigWord: Boolean): Int {
    return SearchHelper.findNextWordEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
      bigWord,
    )
  }
}
