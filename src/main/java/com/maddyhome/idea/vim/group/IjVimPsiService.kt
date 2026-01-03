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
import com.intellij.psi.PsiElement
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
    return getStringLiteralRange(editor, pos, isInner, '"')
      ?: getDoubleQuotesRangeNoPSI(editor.text(), pos, isInner)
  }

  override fun getSingleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange? {
    return getStringLiteralRange(editor, pos, isInner, '\'')
      ?: getSingleQuotesRangeNoPSI(editor.text(), pos, isInner)
  }

  /**
   * Uses PSI to find a string literal at the given position.
   * This handles complex string types like triple-quoted strings, raw strings, and text blocks
   * that the text-based detection cannot properly parse.
   */
  private fun getStringLiteralRange(editor: VimEditor, pos: Int, isInner: Boolean, quoteChar: Char): TextRange? {
    val psiFile = PsiHelper.getFile(editor.ij) ?: return null
    val psiElement = psiFile.findElementAt(pos) ?: return null

    // Walk up the PSI tree looking for a string literal element
    var current: PsiElement? = psiElement
    while (current != null && current !== psiFile) {
      if (isStringLiteralElement(current, quoteChar)) {
        val range = current.textRange
        return if (isInner) {
          getInnerStringRange(current, range)
        } else {
          range.vim
        }
      }
      current = current.parent
    }
    return null
  }

  /**
   * Checks if the given PSI element represents a string literal.
   * Uses the element type's debug name which works across different languages.
   */
  private fun isStringLiteralElement(element: PsiElement, quoteChar: Char): Boolean {
    val elementType = element.node?.elementType?.debugName ?: return false
    val text = element.text

    // Check if the element type indicates a string literal
    // Different languages use different names, but they typically contain "STRING" or "LITERAL"
    val isStringType = elementType.contains("STRING", ignoreCase = true) ||
      (elementType.contains("LITERAL", ignoreCase = true) &&
        !elementType.contains("NUMERIC", ignoreCase = true) &&
        !elementType.contains("BOOLEAN", ignoreCase = true) &&
        !elementType.contains("NULL", ignoreCase = true) &&
        !elementType.contains("INTEGER", ignoreCase = true) &&
        !elementType.contains("FLOAT", ignoreCase = true) &&
        !elementType.contains("DOUBLE", ignoreCase = true) &&
        !elementType.contains("CHAR_LITERAL", ignoreCase = true))

    if (!isStringType) return false

    // Also verify that the element actually starts with the expected quote character
    // This distinguishes between double-quoted and single-quoted strings
    return when (quoteChar) {
      '"' -> text.startsWith("\"") || text.startsWith("@\"") || text.startsWith("r\"") ||
        text.startsWith("r#") || text.startsWith("R\"") || text.startsWith("$\"") ||
        text.startsWith("f\"") || text.startsWith("b\"")
      '\'' -> text.startsWith("'") || text.startsWith("r'") || text.startsWith("b'") ||
        text.startsWith("f'")
      else -> false
    }
  }

  /**
   * Calculates the inner range of a string literal, excluding the quote delimiters.
   * Handles different quote styles: regular quotes, triple quotes, and raw string prefixes.
   */
  private fun getInnerStringRange(element: PsiElement, range: com.intellij.openapi.util.TextRange): TextRange {
    val text = element.text
    val startOffset = findContentStart(text)
    val endOffset = findContentEnd(text)

    val innerStart = range.startOffset + startOffset
    val innerEnd = range.endOffset - endOffset

    // Ensure valid range (inner start must be less than inner end)
    return if (innerStart < innerEnd) {
      TextRange(innerStart, innerEnd)
    } else {
      // Empty string case - return the position between quotes
      TextRange(innerStart, innerStart)
    }
  }

  /**
   * Finds the start offset of the actual string content, after any prefixes and opening quotes.
   */
  private fun findContentStart(text: String): Int {
    // Handle common string prefixes
    var startIndex = 0

    // Skip language-specific prefixes (r, R, f, b, @, $, etc.)
    while (startIndex < text.length && text[startIndex] != '"' && text[startIndex] != '\'') {
      startIndex++
    }

    if (startIndex >= text.length) return text.length

    val quoteChar = text[startIndex]

    // Check for triple quotes
    if (startIndex + 2 < text.length &&
      text[startIndex] == quoteChar &&
      text[startIndex + 1] == quoteChar &&
      text[startIndex + 2] == quoteChar
    ) {
      return startIndex + 3
    }

    // Single quote
    return startIndex + 1
  }

  /**
   * Finds the length of the closing delimiter from the end of the string.
   */
  private fun findContentEnd(text: String): Int {
    if (text.isEmpty()) return 0

    val lastChar = text.last()
    if (lastChar != '"' && lastChar != '\'') return 0

    // Check for triple quotes at the end
    if (text.length >= 3 &&
      text[text.length - 1] == lastChar &&
      text[text.length - 2] == lastChar &&
      text[text.length - 3] == lastChar
    ) {
      return 3
    }

    // Single quote
    return 1
  }
}
