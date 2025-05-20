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
import com.maddyhome.idea.vim.common.InsertSequence
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.helper.currentInsert
import com.maddyhome.idea.vim.helper.insertHistory
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
import com.maddyhome.idea.vim.state.mode.SelectionType

internal class IjVimCaret(val caret: Caret) : VimCaretBase() {

  override val registerStorage: CaretRegisterStorage
    get() {
      var storage = this.caret.registerStorage
      if (storage == null) {
        initInjector() // To initialize injector used in CaretRegisterStorageBase
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
  override val id: String
    get() = caret.hashCode().toString()
  override val editor: VimEditor
    get() = IjVimEditor(caret.editor)
  override val offset: Int
    get() = caret.offset
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

  override fun getLine(): Int {
    return caret.logicalPosition.line
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

  override fun setSelection(start: Int, end: Int) {
    caret.setSelection(start, end)
  }

  override fun removeSelection() {
    caret.removeSelection()
  }

  internal fun getInsertSequenceForTime(time: Long): InsertSequence? {
    val insertHistory = caret.insertHistory
    for (i in insertHistory.lastIndex downTo 0) {
      val insertInfo = insertHistory[i]
      if (time > insertInfo.endNanoTime) return null
      if (time >= insertInfo.startNanoTime) return insertInfo
    }
    return null
  }

  internal fun startInsertSequence(startOffset: Int, startNanoTime: Long) {
    if (caret.currentInsert != null) {
      return
    }
    caret.currentInsert = InsertSequence(startOffset, startNanoTime)
  }

  internal fun endInsertSequence(endInsert: Int, endNanoTime: Long) {
    val currentInsert = caret.currentInsert ?: return
    currentInsert.endNanoTime = endNanoTime
    currentInsert.endOffset = endInsert
    caret.insertHistory.add(currentInsert)
    caret.currentInsert = null
  }

  internal fun abandonCurrentInsertSequece() {
    caret.currentInsert = null
  }

  override fun equals(other: Any?): Boolean = this.caret == (other as? IjVimCaret)?.caret

  override fun hashCode(): Int = this.caret.hashCode()
}

val VimCaret.ij: Caret
  get() = (this as IjVimCaret).caret
val ImmutableVimCaret.ij: Caret
  get() = (this as IjVimCaret).caret

val Caret.vim: VimCaret
  get() = IjVimCaret(this)
