/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.VisualChange
import com.maddyhome.idea.vim.register.Register
import javax.swing.KeyStroke

// TODO: 29.12.2021 Split interface to mutable and immutable
interface VimCaret {
  val editor: VimEditor
  val offset: Offset
  val isValid: Boolean
  val isPrimary: Boolean

  fun getLogicalPosition(): VimLogicalPosition
  fun getVisualPosition(): VimVisualPosition

  fun getLine(): EditorLine.Pointer

  /**
   * Return the buffer line of the caret as a 1-based value, as used by VimScript
   */
  val vimLine: Int

  var vimLastColumn: Int
  fun resetLastColumn()
  val inlayAwareVisualColumn: Int

  fun hasSelection(): Boolean
  val selectionStart: Int
  val selectionEnd: Int
  var vimSelectionStart: Int
  val vimLeadSelectionOffset: Int
  fun vimSetSelection(start: Int, end: Int = start, moveCaretToSelectionEnd: Boolean = false)
  fun vimSetSystemSelectionSilently(start: Int, end: Int)
  fun updateEditorSelection()

  fun setNativeSelection(start: Offset, end: Offset)
  fun removeNativeSelection()

  fun moveToOffset(offset: Int)
  fun moveToOffsetNative(offset: Int)
  fun moveToInlayAwareOffset(newOffset: Int)
  fun moveToVisualPosition(position: VimVisualPosition)
  fun moveToLogicalPosition(logicalPosition: VimLogicalPosition)

  val visualLineStart: Int
  var vimInsertStart: LiveRange
  var vimLastVisualOperatorRange: VisualChange?

  val registerStorage: CaretRegisterStorage
}

interface CaretRegisterStorage {
  // todo methods shouldn't have caret in signature
  /**
   * Stores text to caret's recordable (named/numbered/unnamed) register
   */
  fun storeText(caret: VimCaret, editor: VimEditor, range: TextRange, type: SelectionType, isDelete: Boolean): Boolean

  /**
   * Gets text from caret's recordable register
   * If the register is not recordable - global text state will be returned
   */
  fun getRegister(caret: VimCaret, r: Char): Register?

  fun setKeys(caret: VimCaret, register: Char, keys: List<KeyStroke>)
  fun saveRegister(caret: VimCaret, r: Char, register: Register)
}
