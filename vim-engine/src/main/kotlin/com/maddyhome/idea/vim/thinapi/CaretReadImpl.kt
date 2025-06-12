/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.Range
import com.intellij.vim.api.RegisterData
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.scopes.caret.CaretRead
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.register.Register


class CaretReadImpl(
  override val caretId: CaretId,
) : CaretRead {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override val caretInfo: CaretInfo
    get() {
      val caret: VimCaret = vimEditor.carets().first { it.id == caretId.id }
      return caret.caretInfo
    }

  override fun getCurrentRegisterName(): Char {
    val caretCount: Int = vimEditor.carets().size
    val registerGroup = injector.registerGroup

    val lastRegisterChar: Char =
      if (caretCount == 1) registerGroup.currentRegister else registerGroup.getCurrentRegisterForMulticaret()
    return lastRegisterChar
  }

  override fun getRegisterData(register: Char): RegisterData? {
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return null
    val register: Register = caret.registerStorage.getRegister(vimEditor, context, register) ?: return null
    return RegisterData(register.text, register.type.toRegisterType())
  }

  override fun getRegisterContent(register: Char): String? {
    return getRegisterData(register)?.text
  }

  override fun getRegisterType(register: Char): RegisterType? {
    return getRegisterData(register)?.type
  }

  override fun getVisualSelectionMarks(): Range? {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return null
    return Range(caret.selectionStart, caret.selectionEnd)
  }

  override fun getChangeMarks(): Range? {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return null
    val changeMarks: TextRange = injector.markService.getChangeMarks(caret) ?: return null
    return Range(changeMarks.startOffset, changeMarks.endOffset)
  }

  override fun getCaretLine(): Int {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id }!!
    return caret.getBufferPosition().line
  }
}