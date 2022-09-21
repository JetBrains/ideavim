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
  val registerStorage: CaretRegisterStorage
  val editor: VimEditor
  val offset: Offset
  var vimLastColumn: Int
  val inlayAwareVisualColumn: Int
  val selectionStart: Int
  val selectionEnd: Int
  var vimSelectionStart: Int
  val vimLeadSelectionOffset: Int
  var vimLastVisualOperatorRange: VisualChange?
  val vimLine: Int
  val isPrimary: Boolean
  fun moveToOffset(offset: Int)
  fun moveToOffsetNative(offset: Int)
  fun moveToLogicalPosition(logicalPosition: VimLogicalPosition)
  fun offsetForLineStartSkipLeading(line: Int): Int
  fun getLine(): EditorLine.Pointer
  fun hasSelection(): Boolean
  fun vimSetSystemSelectionSilently(start: Int, end: Int)
  val isValid: Boolean
  fun moveToInlayAwareOffset(newOffset: Int)
  fun vimSetSelection(start: Int, end: Int = start, moveCaretToSelectionEnd: Boolean = false)
  fun getLogicalPosition(): VimLogicalPosition
  fun getVisualPosition(): VimVisualPosition
  val visualLineStart: Int
  fun updateEditorSelection()
  var vimInsertStart: LiveRange
  fun moveToVisualPosition(position: VimVisualPosition)
  fun setNativeSelection(start: Offset, end: Offset)
  fun removeNativeSelection()
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
