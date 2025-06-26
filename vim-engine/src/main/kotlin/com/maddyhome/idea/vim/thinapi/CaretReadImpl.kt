/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.Path
import com.intellij.vim.api.Range
import com.intellij.vim.api.TextType
import com.intellij.vim.api.scopes.caret.CaretRead
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType


class CaretReadImpl(
  override val caretId: CaretId,
) : CaretRead {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

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
      val mode = injector.vimState.mode
      val isVisualBlockMode = mode is Mode.VISUAL && mode.selectionType == SelectionType.BLOCK_WISE

      return if (isVisualBlockMode) {
        val ranges = vimEditor.nativeCarets().mapNotNull { injector.markService.getVisualSelectionMarks(it)?.toRange() }
          .toTypedArray()
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
}
