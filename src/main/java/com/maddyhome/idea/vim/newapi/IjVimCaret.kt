/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.CaretRegisterStorage
import com.maddyhome.idea.vim.api.CaretRegisterStorageBase
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLogicalPosition
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.group.visual.vimLeadSelectionOffset
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.group.visual.vimUpdateEditorSelection
import com.maddyhome.idea.vim.helper.inlayAwareVisualColumn
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.registerStorage
import com.maddyhome.idea.vim.helper.vimInsertStart
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimLastVisualOperatorRange
import com.maddyhome.idea.vim.helper.vimLine
import com.maddyhome.idea.vim.helper.vimSelectionStart

class IjVimCaret(val caret: Caret) : VimCaretBase() {
  override val registerStorage: CaretRegisterStorage
    get() {
      var storage = this.caret.registerStorage
      if (storage == null) {
        storage = CaretRegisterStorageBase()
        this.caret.registerStorage = storage
      }
      return storage
    }
  override val editor: VimEditor
    get() = IjVimEditor(caret.editor)
  override val offset: Offset
    get() = caret.offset.offset
  override var vimLastColumn: Int
    get() = caret.vimLastColumn
    set(value) {
      caret.vimLastColumn = value
    }
  override val inlayAwareVisualColumn: Int
    get() = caret.inlayAwareVisualColumn
  override val selectionStart: Int
    get() = caret.selectionStart
  override val selectionEnd: Int
    get() = caret.selectionEnd
  override var vimSelectionStart: Int
    get() = this.caret.vimSelectionStart
    set(value) {
      this.caret.vimSelectionStart = value
    }
  override val vimLeadSelectionOffset: Int
    get() = this.caret.vimLeadSelectionOffset
  override var vimLastVisualOperatorRange: VisualChange?
    get() = this.caret.vimLastVisualOperatorRange
    set(value) {
      this.caret.vimLastVisualOperatorRange = value
    }
  override val vimLine: Int
    get() = this.caret.vimLine
  override val isPrimary: Boolean
    get() = editor.primaryCaret().ij == this.caret

  override fun moveToOffset(offset: Int) {
    // TODO: 17.12.2021 Unpack internal actions
    MotionGroup.moveCaret(caret.editor, caret, offset)
  }

  override fun moveToOffsetNative(offset: Int) {
    caret.moveToOffset(offset)
  }

  override fun moveToLogicalPosition(logicalPosition: VimLogicalPosition) {
    this.caret.moveToLogicalPosition(LogicalPosition(logicalPosition.line, logicalPosition.column, logicalPosition.leansForward))
  }

  override fun offsetForLineStartSkipLeading(line: Int): Int {
    return VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, line)
  }

  override fun getLine(): EditorLine.Pointer {
    return EditorLine.Pointer.init(caret.logicalPosition.line, editor)
  }

  override fun hasSelection(): Boolean {
    return caret.hasSelection()
  }

  override fun vimSetSystemSelectionSilently(start: Int, end: Int) {
    caret.vimSetSystemSelectionSilently(start, end)
  }

  override val isValid: Boolean
    get() {
      return caret.isValid
    }

  override fun moveToInlayAwareOffset(newOffset: Int) {
    caret.moveToInlayAwareOffset(newOffset)
  }

  override fun vimSetSelection(start: Int, end: Int, moveCaretToSelectionEnd: Boolean) {
    caret.vimSetSelection(start, end, moveCaretToSelectionEnd)
  }

  override fun getLogicalPosition(): VimLogicalPosition {
    val logicalPosition = caret.logicalPosition
    return VimLogicalPosition(logicalPosition.line, logicalPosition.column, logicalPosition.leansForward)
  }

  override fun getVisualPosition(): VimVisualPosition {
    val visualPosition = caret.visualPosition
    return VimVisualPosition(visualPosition.line, visualPosition.column, visualPosition.leansRight)
  }

  override val visualLineStart: Int
    get() = caret.visualLineStart

  override fun updateEditorSelection() {
    caret.vimUpdateEditorSelection()
  }

  override var vimInsertStart: LiveRange
    get() = caret.vimInsertStart.vim
    set(value) {
      caret.vimInsertStart = value.ij
    }

  override fun moveToVisualPosition(position: VimVisualPosition) {
    caret.moveToVisualPosition(VisualPosition(position.line, position.column, position.leansRight))
  }

  override fun setNativeSelection(start: Offset, end: Offset) {
    caret.setSelection(start.point, end.point)
  }

  override fun removeNativeSelection() {
    caret.removeSelection()
  }

  override fun equals(other: Any?): Boolean = this.caret == (other as? IjVimCaret)?.caret

  override fun hashCode(): Int = this.caret.hashCode()
}

val VimCaret.ij: Caret
  get() = (this as IjVimCaret).caret

val Caret.vim: VimCaret
  get() = IjVimCaret(this)
