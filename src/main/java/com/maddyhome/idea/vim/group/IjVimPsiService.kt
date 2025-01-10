/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.components.Service
import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimPsiService
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

@Service
class IjVimPsiService : VimPsiService {
  override fun getCommentAtPos(editor: VimEditor, pos: Int): Pair<TextRange, Pair<String, String>?>? {
    val psiFile = PsiHelper.getFile(editor.ij) ?: return null
    val psiElement = psiFile.findElementAt(pos) ?: return null
    val language = psiElement.language
    val commenter = LanguageCommenters.INSTANCE.forLanguage(language) ?: return null
    val psiComment = PsiTreeUtil.getParentOfType(psiElement, PsiComment::class.java, false) ?: return null
    val commentText = psiComment.text

    val blockCommentPrefix = commenter.blockCommentPrefix
    val blockCommentSuffix = commenter.blockCommentSuffix

    val docCommentPrefix = (commenter as? CodeDocumentationAwareCommenter)?.documentationCommentPrefix
    val docCommentSuffix = (commenter as? CodeDocumentationAwareCommenter)?.documentationCommentSuffix

    val prefixToSuffix: Pair<String, String>? =
      if (docCommentPrefix != null && docCommentSuffix != null && commentText.startsWith(docCommentPrefix) && commentText.endsWith(docCommentSuffix)) {
        docCommentPrefix to docCommentSuffix
      }
      else if (blockCommentPrefix != null && blockCommentSuffix != null && commentText.startsWith(blockCommentPrefix) && commentText.endsWith(blockCommentSuffix)) {
        blockCommentPrefix to blockCommentSuffix
      }
      else {
        null
      }
    return Pair(psiComment.textRange.vim, prefixToSuffix)
  }

  override fun getDoubleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange? {
    // TODO[ideavim] It wasn't implemented before, but implementing it will significantly improve % motion
    return getDoubleQuotesRangeNoPSI(editor.text(), pos, isInner)
  }

  override fun getSingleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange? {
    // TODO[ideavim] It wasn't implemented before, but implementing it will significantly improve % motion
    return getSingleQuotesRangeNoPSI(editor.text(), pos, isInner)
  }
}