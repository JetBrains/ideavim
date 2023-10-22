/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.util.Disposer
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.CaretRegisterStorage
import com.maddyhome.idea.vim.api.CaretRegisterStorageBase
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.LocalMarkStorage
import com.maddyhome.idea.vim.api.SelectionInfo
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.helper.lastSelectionInfo
import com.maddyhome.idea.vim.helper.markStorage
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.registerStorage
import com.maddyhome.idea.vim.helper.resetVimLastColumn
import com.maddyhome.idea.vim.helper.vimInsertStart
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimLastVisualOperatorRange
import com.maddyhome.idea.vim.helper.vimLine
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.helper.vimSelectionStartClear

internal class IjVimCaret(val caret: Caret) : VimCaretBase() {

  init {
    Disposer.register(caret) {
      (registerStorage as CaretRegisterStorageBase).clearListener()
    }
  }

  override val registerStorage: CaretRegisterStorage
    get() {
      var storage = this.caret.registerStorage
      if (storage == null) {
        storage = CaretRegisterStorageBase(this)
        this.caret.registerStorage = storage
      } else if (storage.caret != this) {
        storage.caret = this
      }
      return storage
    }
  override val markStorage: LocalMarkStorage
    get() {
      var storage = this.caret.markStorage
      if (storage == null) {
        storage = LocalMarkStorage(this)
        this.caret.markStorage = storage
      } else if (storage.caret != this) {
        storage.caret = this
      }
      return storage
    }
  override var lastSelectionInfo: SelectionInfo
    get() {
      val lastSelection = this.caret.lastSelectionInfo
      if (lastSelection == null) {
        val defaultValue = SelectionInfo(null, null, SelectionType.CHARACTER_WISE)
        this.caret.lastSelectionInfo = defaultValue
        return defaultValue
      }
      return lastSelection
    }
    set(value) {
      this.caret.lastSelectionInfo = value
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

  override fun resetLastColumn() = caret.resetVimLastColumn()
  override val selectionStart: Int
    get() = caret.selectionStart
  override val selectionEnd: Int
    get() = caret.selectionEnd
  override var vimSelectionStart: Int
    get() = this.caret.vimSelectionStart
    set(value) {
      this.caret.vimSelectionStart = value
    }

  override fun vimSelectionStartClear() {
    this.caret.vimSelectionStartClear()
  }

  override var vimLastVisualOperatorRange: VisualChange?
    get() = this.caret.vimLastVisualOperatorRange
    set(value) {
      this.caret.vimLastVisualOperatorRange = value
    }
  override val vimLine: Int
    get() = this.caret.vimLine
  override val isPrimary: Boolean
    get() = editor.primaryCaret().ij == this.caret

  override fun moveToOffsetNative(offset: Int) {
    caret.moveToOffset(offset)
  }

  override fun moveToBufferPosition(position: BufferPosition) {
    this.caret.moveToLogicalPosition(LogicalPosition(position.line, position.column, position.leansForward))
  }

  override fun getLine(): EditorLine.Pointer {
    return EditorLine.Pointer.init(caret.logicalPosition.line, editor)
  }

  override fun hasSelection(): Boolean {
    return caret.hasSelection()
  }

  override val isValid: Boolean
    get() {
      return caret.isValid
    }

  override fun moveToInlayAwareOffset(newOffset: Int): VimCaret {
    caret.moveToInlayAwareOffset(newOffset)
    return this
  }

  override fun getBufferPosition(): BufferPosition {
    val logicalPosition = caret.logicalPosition
    return BufferPosition(logicalPosition.line, logicalPosition.column, logicalPosition.leansForward)
  }

  override fun getVisualPosition(): VimVisualPosition {
    val visualPosition = caret.visualPosition
    return VimVisualPosition(visualPosition.line, visualPosition.column, visualPosition.leansRight)
  }

  override val visualLineStart: Int
    get() = caret.visualLineStart

  override var vimInsertStart: LiveRange
    get() = caret.vimInsertStart.vim
    set(value) {
      caret.vimInsertStart = value.ij
    }

  override fun moveToVisualPosition(position: VimVisualPosition) {
    caret.moveToVisualPosition(VisualPosition(position.line, position.column, position.leansRight))
  }

  override fun setVimLastColumnAndGetCaret(col: Int): VimCaret {
    caret.vimLastColumn = col
    return this
  }

  override fun setSelection(start: Offset, end: Offset) {
    caret.setSelection(start.point, end.point)
  }

  override fun removeSelection() {
    caret.removeSelection()
  }

  override fun equals(other: Any?): Boolean = this.caret == (other as? IjVimCaret)?.caret

  override fun hashCode(): Int = this.caret.hashCode()
}

public val VimCaret.ij: Caret
  get() = (this as IjVimCaret).caret
public val ImmutableVimCaret.ij: Caret
  get() = (this as IjVimCaret).caret

public val Caret.vim: VimCaret
  get() = IjVimCaret(this)
