/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor.caret

import com.intellij.vim.api.models.CaretId
import com.intellij.vim.api.models.Line
import com.intellij.vim.api.models.Mark
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.models.TextType
import com.intellij.vim.api.scopes.editor.caret.CaretRead
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimBlockSelectionRenderer
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.thinapi.toApiMark
import com.maddyhome.idea.vim.thinapi.toRange
import com.maddyhome.idea.vim.thinapi.toTextSelectionType


class CaretReadImpl(
  private val projectId: String?,
  override val caretId: CaretId,
) : CaretRead {
  private val vimEditor: VimEditor
    get() = projectId?.let { injector.editorGroup.getSelectedEditor(it) } ?: injector.fallbackWindow

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private val vimCaret: VimCaret
    get() = vimEditor.carets().first { it.id == caretId.id }

  private val registerGroup
    get() = injector.registerGroup

  override val offset: Int
    get() = vimCaret.offset

  override val selection: Range
    get() {
      if (!isVisualBlockMode()) {
        return Range.Simple(vimCaret.selectionStart, vimCaret.selectionEnd)
      }
      val bounds = injector.blockSelectionRenderer.getBounds(vimEditor)
        ?: return blockRangeFromNativeCarets()
      return blockRangeFromBounds(bounds)
    }

  private fun isVisualBlockMode(): Boolean {
    val mode = injector.vimState.mode
    return mode is Mode.VISUAL && mode.selectionType == SelectionType.BLOCK_WISE
  }

  private fun blockRangeFromBounds(bounds: VimBlockSelectionRenderer.BlockBounds): Range.Block {
    val minLine = minOf(bounds.anchorLine, bounds.activeLine)
    val maxLine = maxOf(bounds.anchorLine, bounds.activeLine)
    val minCol = minOf(bounds.anchorCol, bounds.activeCol)
    val maxCol = maxOf(bounds.anchorCol, bounds.activeCol)
    val start = rowOffsetClampedToLineEnd(minLine, minCol)
    val end = rowOffsetClampedToLineEnd(maxLine, maxCol)
    return Range.Block(start, end)
  }

  private fun rowOffsetClampedToLineEnd(line: Int, column: Int): Int {
    val rowStart = vimEditor.getLineStartOffset(line)
    val rowEnd = vimEditor.getLineEndOffset(line)
    return (rowStart + column).coerceAtMost(rowEnd)
  }

  // Fallback (should be rare): aggregate whatever native carets exist.
  private fun blockRangeFromNativeCarets(): Range.Block {
    val allCarets = vimEditor.nativeCarets()
    val start = allCarets.minOf { it.selectionStart }
    val end = allCarets.maxOf { it.selectionEnd }
    return Range.Block(start, end)
  }

  override val line: Line
    get() {
      val lineNumber = vimCaret.getLine()
      val lineText = vimEditor.getLineText(lineNumber)
      val lineStartOffset = vimEditor.getLineStartOffset(lineNumber)
      val lineEndOffset = vimEditor.getLineEndOffset(lineNumber)
      return Line(lineNumber, lineText, lineStartOffset, lineEndOffset)
    }

  override val lastSelectedReg: Char
    get() {
      val caretCount: Int = vimEditor.carets().size
      val lastRegisterChar: Char =
        if (caretCount == 1) registerGroup.currentRegister else registerGroup.getCurrentRegisterForMulticaret()
      return lastRegisterChar
    }

  override val defaultRegister: Char
    get() = registerGroup.defaultRegister

  override val isRegisterSpecifiedExplicitly: Boolean
    get() = registerGroup.isRegisterSpecifiedExplicitly

  override fun selectRegister(register: Char): Boolean {
    return registerGroup.selectRegister(register)
  }

  override fun resetRegisters() {
    registerGroup.resetRegisters()
  }

  override fun isWritable(register: Char): Boolean {
    return registerGroup.isRegisterWritable(register)
  }

  override fun isSystemClipboard(register: Char): Boolean {
    return registerGroup.isSystemClipboard(register)
  }

  override fun isPrimaryRegisterSupported(): Boolean {
    return registerGroup.isPrimaryRegisterSupported()
  }

  override val selectionMarks: Range?
    get() {
      if (isVisualBlockMode()) {
        val marks = injector.markService.getVisualSelectionMarks(vimEditor.primaryCaret()) ?: return null
        return Range.Block(marks.startOffset, marks.endOffset)
      }
      return injector.markService.getVisualSelectionMarks(vimCaret)?.toRange()
    }

  override val changeMarks: Range?
    get() {
      return injector.markService.getChangeMarks(vimCaret)?.toRange()
    }

  private data class RegisterData(
    val text: String,
    val type: TextType,
  )

  private fun getRegisterData(register: Char): RegisterData? {
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return null
    val register: Register = caret.registerStorage.getRegister(vimEditor, context, register) ?: return null
    return RegisterData(register.text, register.type.toTextSelectionType())
  }

  override fun getReg(register: Char): String? {
    return getRegisterData(register)?.text
  }

  override fun getRegType(register: Char): TextType? {
    return getRegisterData(register)?.type
  }

  override fun setReg(register: Char, text: String, textType: TextType): Boolean {
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    return when (textType) {
      TextType.CHARACTER_WISE -> registerGroup.storeText(
        vimEditor,
        context,
        register,
        text,
        SelectionType.CHARACTER_WISE
      )

      TextType.LINE_WISE -> registerGroup.storeText(vimEditor, context, register, text, SelectionType.LINE_WISE)
      TextType.BLOCK_WISE -> registerGroup.storeText(vimEditor, context, register, text, SelectionType.BLOCK_WISE)
    }
  }

  override fun getMark(char: Char): Mark? {
    val mark = injector.markService.getMark(vimCaret, char)
    return mark?.toApiMark()
  }

  override val localMarks: Set<Mark>
    get() = injector.markService.getAllLocalMarks(vimCaret).map { it.toApiMark() }.toSet()

  override fun setMark(char: Char): Boolean {
    return injector.markService.setMark(vimCaret, char, vimCaret.offset)
  }

  override fun setMark(char: Char, offset: Int): Boolean {
    return injector.markService.setMark(vimCaret, char, offset)
  }

  override fun removeLocalMark(char: Char) {
    injector.markService.removeLocalMark(vimCaret, char)
  }

  override fun resetAllMarksForCaret() {
    injector.markService.resetAllMarksForCaret(vimCaret)
  }

  override fun scrollFullPage(pages: Int): Boolean {
    return injector.scroll.scrollFullPage(vimEditor, vimCaret, pages)
  }

  override fun scrollHalfPageUp(lines: Int): Boolean {
    return injector.scroll.scrollHalfPage(vimEditor, vimCaret, lines, false)
  }

  override fun scrollHalfPageDown(lines: Int): Boolean {
    return injector.scroll.scrollHalfPage(vimEditor, vimCaret, lines, true)
  }

  // Window selection methods commented out — see IJPL-235369.
  //
  // override fun selectWindowHorizontally(relativePosition: Int) {
  //   injector.window.selectWindowInRow(vimCaret, vimContext, relativePosition, false)
  // }
  //
  // override fun selectWindowInVertically(relativePosition: Int) {
  //   injector.window.selectWindowInRow(vimCaret, vimContext, relativePosition, true)
  // }

  override fun getNextParagraphBoundOffset(count: Int, includeWhitespaceLines: Boolean): Int? {
    return injector.searchHelper.findNextParagraph(vimEditor, vimCaret, count, includeWhitespaceLines)
  }

  override fun getNextSentenceStart(count: Int, includeCurrent: Boolean, requireAll: Boolean): Int? {
    return injector.searchHelper.findNextSentenceStart(vimEditor, vimCaret, count, includeCurrent, requireAll)
  }

  override fun getNextSectionStart(marker: Char, count: Int): Int {
    return injector.searchHelper.findSection(vimEditor, vimCaret, marker, 1, count)
  }

  override fun getPreviousSectionStart(marker: Char, count: Int): Int {
    return injector.searchHelper.findSection(vimEditor, vimCaret, marker, -1, count)
  }

  override fun getNextSentenceEnd(count: Int, includeCurrent: Boolean, requireAll: Boolean): Int? {
    return injector.searchHelper.findNextSentenceEnd(vimEditor, vimCaret, count, includeCurrent, requireAll)
  }

  override fun getMethodEndOffset(count: Int): Int {
    return injector.searchHelper.findMethodEnd(vimEditor, vimCaret, count)
  }

  override fun getMethodStartOffset(count: Int): Int {
    return injector.searchHelper.findMethodStart(vimEditor, vimCaret, count)
  }

  override fun getNextCharOnLineOffset(count: Int, char: Char): Int {
    return injector.searchHelper.findNextCharacterOnLine(vimEditor, vimCaret, count, char)
  }

  override fun getCurrentOrFollowingWord(isBigWord: Boolean): Range? {
    val textRange = injector.searchHelper.findWordAtOrFollowingCursor(vimEditor, vimCaret, isBigWord)
    return textRange?.toRange()
  }

  override fun getWordTextObjectRange(count: Int, isOuter: Boolean, isBigWord: Boolean): Range {
    val textRange = injector.searchHelper.findWordObject(vimEditor, vimCaret, count, isOuter, isBigWord)
    return textRange.toRange()
  }

  override fun getSentenceRange(count: Int, isOuter: Boolean): Range {
    val textRange = injector.searchHelper.findSentenceRange(vimEditor, vimCaret, count, isOuter)
    return textRange.toRange()
  }

  override fun getParagraphRange(count: Int, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findParagraphRange(vimEditor, vimCaret, count, isOuter)
    return textRange?.toRange()
  }

  override fun getBlockTagRange(count: Int, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findBlockTagRange(vimEditor, vimCaret, count, isOuter)
    return textRange?.toRange()
  }

  override fun getBlockQuoteInLineRange(quote: Char, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findBlockQuoteInLineRange(vimEditor, vimCaret, quote, isOuter)
    return textRange?.toRange()
  }

  override fun getNextMisspelledWordOffset(count: Int): Int {
    return injector.searchHelper.findMisspelledWord(vimEditor, vimCaret, count)
  }
}
