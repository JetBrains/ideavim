/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.LanguageCommenters
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimPsiService
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim


class IjVimPsiService : VimPsiService {
  override fun hasLanguageIndentSupport(editor: VimEditor): Boolean {
    val psiFile = PsiHelper.getFile(editor.ij) ?: return false
    // A formatter is what lets the Platform work the indent out from the code. Without one, the new line action can
    // only repeat the indent of the previous line, which is exactly what 'autoindent' does
    return LanguageFormatting.INSTANCE.forContext(psiFile) != null
  }

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
      if (docCommentPrefix != null && docCommentSuffix != null && commentText.startsWith(docCommentPrefix) && commentText.endsWith(
          docCommentSuffix
        )
      ) {
        docCommentPrefix to docCommentSuffix
      } else if (blockCommentPrefix != null && blockCommentSuffix != null && commentText.startsWith(blockCommentPrefix) && commentText.endsWith(
          blockCommentSuffix
        )
      ) {
        blockCommentPrefix to blockCommentSuffix
      } else {
        null
      }
    return Pair(psiComment.textRange.vim, prefixToSuffix)
  }

  override fun getDoubleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange? {
    val range = getDoubleQuotesRangeNoPSI(editor.text(), pos, isInner) ?: return null
    return if (isQuotedRangeConfirmedByPsi(editor, range, isInner)) range else null
  }

  override fun getSingleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange? {
    val range = getSingleQuotesRangeNoPSI(editor.text(), pos, isInner) ?: return null
    return if (isQuotedRangeConfirmedByPsi(editor, range, isInner)) range else null
  }

  /**
   * The text-based search treats any two quote characters on a line as a string. This is wrong for languages where
   * quotes can be a part of other tokens, e.g., identifiers with primes like `f'` in Haskell-like languages.
   * If PSI is available, we check that the opening quote starts a token and the closing quote ends a token.
   * Quotes in plain text and comments are not tokenized, so we keep the text-based behaviour there.
   */
  private fun isQuotedRangeConfirmedByPsi(editor: VimEditor, range: TextRange, isInner: Boolean): Boolean {
    val psiFile = PsiHelper.getFile(editor.ij) ?: return true
    if (psiFile.language == PlainTextLanguage.INSTANCE) return true

    val openQuote = if (isInner) range.startOffset - 1 else range.startOffset
    val closeQuote = if (isInner) range.endOffset else range.endOffset - 1
    val openElement = psiFile.findElementAt(openQuote) ?: return true
    if (PsiTreeUtil.getParentOfType(openElement, PsiComment::class.java, false) != null) return true
    val closeElement = psiFile.findElementAt(closeQuote) ?: return true
    return openElement.textRange.startOffset == openQuote && closeElement.textRange.endOffset == closeQuote + 1
  }

  override fun getCommentBlockRange(editor: VimEditor, cursorLine: Int): TextRange? {
    val nativeEditor = editor.ij
    val file = PsiHelper.getFile(nativeEditor) ?: return null
    val lastLine = editor.lineCount()

    var startLine = cursorLine
    while (startLine > 0 && isCommentLine(file, nativeEditor, startLine - 1)) startLine--
    var endLine = cursorLine - 1
    while (endLine < lastLine && isCommentLine(file, nativeEditor, endLine + 1)) endLine++

    if (startLine <= endLine) {
      val startOffset = editor.getLineStartOffset(startLine)
      val endOffset = editor.getLineStartOffset(endLine + 1)
      return TextRange(startOffset, endOffset)
    }

    return null
  }

  /**
   * Check all leaf nodes in the given line are whitespace, comments, or are owned by comments.
   */
  private fun isCommentLine(file: PsiFile, editor: Editor, logicalLine: Int): Boolean {
    val startOffset = editor.vim.getLineStartOffset(logicalLine)
    val endOffset = editor.vim.getLineEndOffset(logicalLine, true)
    val startElement = file.findElementAt(startOffset) ?: return false
    var next: PsiElement? = startElement
    var hasComment = false
    while (next != null && next.textRange.startOffset <= endOffset) {
      when {
        next is PsiWhiteSpace -> {} // Skip whitespace element
        isComment(next) -> hasComment = true // Mark when we find a comment
        else -> return false // Non-comment content found, exit early
      }
      next = PsiTreeUtil.nextLeaf(next, true)
    }
    return hasComment
  }

  private fun isComment(element: PsiElement) =
    PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null
}