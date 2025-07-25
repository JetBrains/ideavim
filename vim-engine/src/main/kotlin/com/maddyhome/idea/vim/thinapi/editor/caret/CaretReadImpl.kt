/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor.caret

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.Range
import com.intellij.vim.api.TextType
import com.intellij.vim.api.scopes.editor.caret.CaretRead
import com.maddyhome.idea.vim.api.ExecutionContext
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
  override val caretId: CaretId,
) : CaretRead {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

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
      val mode = injector.vimState.mode
      val isVisualBlockMode = mode is Mode.VISUAL && mode.selectionType == SelectionType.BLOCK_WISE

      return if (isVisualBlockMode) {
        val ranges = vimEditor.nativeCarets().map { Range.Simple(it.selectionStart, it.selectionEnd) }
          .toTypedArray()
        Range.Block(ranges)
      } else {
        Range.Simple(vimCaret.selectionStart, vimCaret.selectionEnd)
      }
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

  override suspend fun selectRegister(register: Char): Boolean {
    return registerGroup.selectRegister(register)
  }

  override suspend fun resetRegisters() {
    registerGroup.resetRegisters()
  }

  override suspend fun isWritable(register: Char): Boolean {
    return registerGroup.isRegisterWritable(register)
  }

  override suspend fun isSystemClipboard(register: Char): Boolean {
    return registerGroup.isSystemClipboard(register)
  }

  override suspend fun isPrimaryRegisterSupported(): Boolean {
    return registerGroup.isPrimaryRegisterSupported()
  }

  override val selectionMarks: Range?
    get() {
      val mode = injector.vimState.mode
      val isVisualBlockMode = mode is Mode.VISUAL && mode.selectionType == SelectionType.BLOCK_WISE

      return if (isVisualBlockMode) {
        val ranges = vimEditor.nativeCarets().mapNotNull { 
          val marks = injector.markService.getVisualSelectionMarks(it) ?: return@mapNotNull null
          Range.Simple(marks.startOffset, marks.endOffset)
        }.toTypedArray()
        Range.Block(ranges)
      } else {
        val visualSelectionMarks = injector.markService.getVisualSelectionMarks(vimCaret) ?: return null
        visualSelectionMarks.toRange()
      }
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

  override suspend fun getReg(register: Char): String? {
    return getRegisterData(register)?.text
  }

  override suspend fun getRegType(register: Char): TextType? {
    return getRegisterData(register)?.type
  }

  override suspend fun setReg(register: Char, text: String, textType: TextType): Boolean {
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

  override suspend fun getMark(char: Char): Mark? {
    val mark = injector.markService.getMark(vimCaret, char)
    return mark?.toApiMark()
  }

  override val localMarks: Set<Mark>
    get() = injector.markService.getAllLocalMarks(vimCaret).map { it.toApiMark() }.toSet()

  override suspend fun setMark(char: Char): Boolean {
    return injector.markService.setMark(vimCaret, char, vimCaret.offset)
  }

  override suspend fun setMark(char: Char, offset: Int): Boolean {
    return injector.markService.setMark(vimCaret, char, offset)
  }

  override suspend fun removeLocalMark(char: Char) {
    injector.markService.removeLocalMark(vimCaret, char)
  }

  override suspend fun resetAllMarksForCaret() {
    injector.markService.resetAllMarksForCaret(vimCaret)
  }

  override suspend fun scrollFullPage(pages: Int): Boolean {
    return injector.scroll.scrollFullPage(vimEditor, vimCaret, pages)
  }

  override suspend fun scrollHalfPageUp(lines: Int): Boolean {
    return injector.scroll.scrollHalfPage(vimEditor, vimCaret, lines, false)
  }

  override suspend fun scrollHalfPageDown(lines: Int): Boolean {
    return injector.scroll.scrollHalfPage(vimEditor, vimCaret, lines, true)
  }

  override suspend fun selectWindowHorizontally(relativePosition: Int) {
    injector.window.selectWindowInRow(vimCaret, vimContext, relativePosition, false)
  }

  override suspend fun selectWindowInVertically(relativePosition: Int) {
    injector.window.selectWindowInRow(vimCaret, vimContext, relativePosition, true)
  }

  override suspend fun getNextParagraphBoundOffset(count: Int, includeWhitespaceLines: Boolean): Int? {
    return injector.searchHelper.findNextParagraph(vimEditor, vimCaret, count, includeWhitespaceLines)
  }

  override suspend fun getNextSentenceStart(count: Int, includeCurrent: Boolean, requireAll: Boolean): Int? {
    return injector.searchHelper.findNextSentenceStart(vimEditor, vimCaret, count, includeCurrent, requireAll)
  }

  override suspend fun getNextSectionStart(marker: Char, count: Int): Int {
    return injector.searchHelper.findSection(vimEditor, vimCaret, marker, 1, count)
  }

  override suspend fun getPreviousSectionStart(marker: Char, count: Int): Int {
    return injector.searchHelper.findSection(vimEditor, vimCaret, marker, -1, count)
  }

  override suspend fun getNextSentenceEnd(count: Int, includeCurrent: Boolean, requireAll: Boolean): Int? {
    return injector.searchHelper.findNextSentenceEnd(vimEditor, vimCaret, count, includeCurrent, requireAll)
  }

  override suspend fun getMethodEndOffset(count: Int): Int {
    return injector.searchHelper.findMethodEnd(vimEditor, vimCaret, count)
  }

  override suspend fun getMethodStartOffset(count: Int): Int {
    return injector.searchHelper.findMethodStart(vimEditor, vimCaret, count)
  }

  override suspend fun getNextCharOnLineOffset(count: Int, char: Char): Int {
    return injector.searchHelper.findNextCharacterOnLine(vimEditor, vimCaret, count, char)
  }

  override suspend fun getNearestWordOffset(): Range? {
    val textRange = injector.searchHelper.findWordNearestCursor(vimEditor, vimCaret)
    return textRange?.toRange()
  }

  override suspend fun getWordTextObjectRange(count: Int, isOuter: Boolean, isBigWord: Boolean): Range {
    val textRange = injector.searchHelper.findWordObject(vimEditor, vimCaret, count, isOuter, isBigWord)
    return textRange.toRange()
  }

  override suspend fun getSentenceRange(count: Int, isOuter: Boolean): Range {
    val textRange = injector.searchHelper.findSentenceRange(vimEditor, vimCaret, count, isOuter)
    return textRange.toRange()
  }

  override suspend fun getParagraphRange(count: Int, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findParagraphRange(vimEditor, vimCaret, count, isOuter)
    return textRange?.toRange()
  }

  override suspend fun getBlockTagRange(count: Int, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findBlockTagRange(vimEditor, vimCaret, count, isOuter)
    return textRange?.toRange()
  }

  override suspend fun getBlockQuoteInLineRange(quote: Char, isOuter: Boolean): Range? {
    val textRange = injector.searchHelper.findBlockQuoteInLineRange(vimEditor, vimCaret, quote, isOuter)
    return textRange?.toRange()
  }

  override suspend fun getNextMisspelledWordOffset(count: Int): Int {
    return injector.searchHelper.findMisspelledWord(vimEditor, vimCaret, count)
  }
}
