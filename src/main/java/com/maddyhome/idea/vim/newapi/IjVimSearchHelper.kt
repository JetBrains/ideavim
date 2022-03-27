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
}
