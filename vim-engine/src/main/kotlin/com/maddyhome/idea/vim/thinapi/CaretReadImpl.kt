/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
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

  override val offset: Int
    get() = vimCaret.offset

  override val selection: Array<Range>
    get() {
      return vimEditor.nativeCarets().map { Range(it.selectionStart, it.selectionEnd) }.toTypedArray()
    }

  override val line: Int
    get() = vimCaret.getLine()

  override val lastSelectedReg: Char
    get() {
      val caretCount: Int = vimEditor.carets().size
      val registerGroup = injector.registerGroup

      val lastRegisterChar: Char =
        if (caretCount == 1) registerGroup.currentRegister else registerGroup.getCurrentRegisterForMulticaret()
      return lastRegisterChar
    }

  override val visualSelectionMarks: Array<Range>?
    get() {
      val mode = injector.vimState.mode
      val isVisualBlockMode = mode is Mode.VISUAL && mode.selectionType == SelectionType.BLOCK_WISE

      return if (isVisualBlockMode) {
        vimEditor.nativeCarets().map { Range(it.selectionStart, it.selectionEnd) }
          .toTypedArray()
      } else {
        arrayOf(Range(vimCaret.selectionStart, vimCaret.selectionEnd))
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
}