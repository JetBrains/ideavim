/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Jump
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.Range
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.caret.CaretRead
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.key.MappingOwner

open class ReadImpl(
  listenerOwner: ListenerOwner,
  mappingOwner: MappingOwner,
) : Read, VimScopeImpl(listenerOwner, mappingOwner) {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override val textLength: Long
    get() = vimEditor.fileSize()
  override val text: CharSequence
    get() = vimEditor.text()
  override val lineCount: Int
    get() = vimEditor.lineCount()

  override fun <T> forEachCaret(block: CaretRead.() -> T): List<T> {
    return vimEditor.sortedCarets().map { caret -> CaretReadImpl(caret.caretId).block() }
  }

  override fun with(
    caretId: CaretId,
    block: CaretRead.() -> Unit,
  ) {
    CaretReadImpl(caretId).block()
  }

  override fun withPrimaryCaret(block: CaretRead.() -> Unit) {
    CaretReadImpl(vimEditor.primaryCaret().caretId).block()
  }

  override fun getLineStartOffset(line: Int): Int {
    return vimEditor.getLineStartOffset(line)
  }

  override fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return vimEditor.getLineEndOffset(line, allowEnd)
  }

  override fun getLine(offset: Int): Line {
    val lineNumber = vimEditor.offsetToBufferPosition(offset).line
    val lineText = vimEditor.getLineText(lineNumber)
    val lineStartOffset = vimEditor.getLineStartOffset(lineNumber)
    val lineEndOffset = vimEditor.getLineEndOffset(lineNumber)
    return Line(lineNumber, lineText, lineStartOffset, lineEndOffset)
  }

  override val caretData: List<CaretData>
    get() = vimEditor.sortedCarets().map { caret -> caret.caretId to caret.caretInfo }
  override val caretIds: List<CaretId>
    get() = vimEditor.sortedCarets().map { caret -> caret.caretId }

  override fun getGlobalMark(char: Char): Mark? {
    val mark = injector.markService.getGlobalMark(char)
    return mark?.toApiMark()
  }

  override val globalMarks: Set<Mark>
    get() = injector.markService.getAllGlobalMarks().map { it.toApiMark() }.toSet()

  override fun getJump(count: Int): Jump? {
    val jump = injector.jumpService.getJump(vimEditor.projectId, count)
    return jump?.toApiJump()
  }

  override val jumps: List<Jump>
    get() = injector.jumpService.getJumps(vimEditor.projectId).map { it.toApiJump() }

  override val currentJumpIndex: Int
    get() = injector.jumpService.getJumpSpot(vimEditor.projectId)

  override fun scrollCaretIntoView() {
    return injector.scroll.scrollCaretIntoView(vimEditor)
  }

  override fun scrollVertically(lines: Int): Boolean {
    return injector.scroll.scrollLines(vimEditor, lines)
  }

  override fun scrollLineToTop(line: Int, start: Boolean): Boolean {
    return injector.scroll.scrollCurrentLineToDisplayTop(vimEditor, line, start)
  }

  override fun scrollLineToMiddle(line: Int, start: Boolean): Boolean {
    return injector.scroll.scrollCurrentLineToDisplayMiddle(vimEditor, line, start)
  }

  override fun scrollLineToBottom(line: Int, start: Boolean): Boolean {
    return injector.scroll.scrollCurrentLineToDisplayBottom(vimEditor, line, start)
  }

  override fun scrollHorizontally(columns: Int): Boolean {
    return injector.scroll.scrollColumns(vimEditor, columns)
  }

  override fun scrollCaretToLeftEdge(): Boolean {
    return injector.scroll.scrollCaretColumnToDisplayLeftEdge(vimEditor)
  }

  override fun scrollCaretToRightEdge(): Boolean {
    return injector.scroll.scrollCaretColumnToDisplayRightEdge(vimEditor)
  }

  override fun getNextParagraphBoundOffset(startLine: Int, count: Int, includeWhitespaceLines: Boolean): Int? {
    return injector.searchHelper.findNextParagraph(vimEditor, startLine, count, includeWhitespaceLines)
  }

  override fun getNextSentenceStart(startOffset: Int, count: Int, includeCurrent: Boolean, requireAll: Boolean): Int? {
    return injector.searchHelper.findNextSentenceStart(vimEditor, startOffset, count, includeCurrent, requireAll)
  }

  override fun getNextSectionStart(startLine: Int, marker: Char, count: Int): Int {
    return injector.searchHelper.findSection(vimEditor, startLine, marker, 1, count)
  }

  override fun getPreviousSectionStart(startLine: Int, marker: Char, count: Int): Int {
    return injector.searchHelper.findSection(vimEditor, startLine, marker, -1, count)
  }

  override fun getNextSentenceEnd(startOffset: Int, count: Int, includeCurrent: Boolean, requireAll: Boolean): Int? {
    return injector.searchHelper.findNextSentenceEnd(vimEditor, startOffset, count, includeCurrent, requireAll)
  }

  override fun getNextWordStartOffset(startOffset: Int, count: Int, isBigWord: Boolean): Int {
    return injector.searchHelper.findNextWord(vimEditor, startOffset, count, isBigWord)
  }

  override fun getNextWordEndOffset(startOffset: Int, count: Int, isBigWord: Boolean, stopOnEmptyLine: Boolean): Int {
    return injector.searchHelper.findNextWordEnd(vimEditor, startOffset, count, isBigWord, stopOnEmptyLine)
  }

  override fun getNextCharOnLineOffset(startOffset: Int, count: Int, char: Char): Int {
    return injector.searchHelper.findNextCharacterOnLine(vimEditor, startOffset, count, char)
  }

  override fun getNearestWordOffset(startOffset: Int): Range? {
    val textRange = injector.searchHelper.findWordNearestCursor(vimEditor, startOffset)
    return textRange?.toRange()
  }

  override fun getParagraphRange(line: Int, count: Int, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findParagraphRange(vimEditor, line, count, isOuter)
    return textRange?.toRange()
  }

  override fun getBlockQuoteInLineRange(startOffset: Int, quote: Char, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findBlockQuoteInLineRange(vimEditor, startOffset, quote, isOuter)
    return textRange?.toRange()
  }

  override fun findAll(
    pattern: String,
    startLine: Int,
    endLine: Int,
    ignoreCase: Boolean,
  ): List<Range> {
    val textRanges = injector.searchHelper.findAll(vimEditor, pattern, startLine, endLine, ignoreCase)
    return textRanges.map { it.toRange() }
  }

  override fun findPattern(
    pattern: String,
    startOffset: Int,
    count: Int,
    backwards: Boolean,
  ): Range? {
    val vimSearchOptions = if (backwards) enumSetOf(SearchOptions.BACKWARDS) else enumSetOf()
    val textRange = injector.searchHelper.findPattern(vimEditor, pattern, startOffset, count, vimSearchOptions)
    return textRange?.toRange()
  }

}