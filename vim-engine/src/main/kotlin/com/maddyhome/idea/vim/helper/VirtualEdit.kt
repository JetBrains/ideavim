/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

/**
 * 'virtualedit' lets the caret be placed after the end of a line, in virtual space.
 *
 * The caret keeps the offset of the line end there, and [virtualColumns] tells how far after it the caret is. The option
 * only decides whether a motion can move the caret into virtual space, see [isVirtualEditActive]. While it is active,
 * the end of a line is never a character, see [isVirtualLineEnd]. A command that changes text at the caret fills the
 * virtual space with spaces first, see [fillVirtualSpace].
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.action.change.VimRedoBuffer
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.moveToMotion
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

fun VimEditor.isVirtualEditActive(mode: Mode = this.mode): Boolean {
  val virtualedit = injector.options(this).virtualedit
  if (mode.selectionType == SelectionType.BLOCK_WISE) return false
  return OptionConstants.virtualedit_all in virtualedit ||
    (OptionConstants.virtualedit_insert in virtualedit && mode.isInsertOrPending)
}

/**
 * True for Insert and Replace modes, and for the modes entered from them for a while, e.g. with `<C-O>`
 */
private val Mode.isInsertOrPending: Boolean
  get() = generateSequence(this) { mode -> mode.returnTo.takeIf { it != mode } }
    .any { it is Mode.INSERT || it == Mode.REPLACE }

/**
 * True if a caret at or after the end of a line can stay there, instead of being moved back to the last character
 */
fun VimEditor.allowsCaretAtLineEnd(mode: Mode = this.mode): Boolean = isEndAllowed(mode) || isVirtualEditActive(mode)

/**
 * True if 'virtualedit' allows the caret after the line end in some mode, so a caret there may be on its way back
 */
val VimEditor.usesVirtualEdit: Boolean
  get() {
    val virtualedit = injector.options(this).virtualedit
    return OptionConstants.virtualedit_all in virtualedit || OptionConstants.virtualedit_insert in virtualedit
  }

fun VimEditor.isVirtualLineEnd(offset: Int, mode: Mode = this.mode): Boolean =
  isVirtualEditActive(mode) && offset == getLineEndForOffset(offset)

/**
 * True if there is no character under the caret: 'virtualedit' is active and the caret is at or after the line end
 */
val ImmutableVimCaret.isInVirtualSpace: Boolean
  get() = editor.isVirtualLineEnd(offset)

/**
 * The number of columns between the end of the line and the caret, 0 if the caret is not after the line end
 */
val ImmutableVimCaret.virtualColumns: Int
  get() {
    val lineEndOffset = editor.getLineEndForOffset(offset)
    if (offset != lineEndOffset) return 0
    return (getBufferPosition().column - editor.offsetToBufferPosition(lineEndOffset).column).coerceAtLeast(0)
  }

/**
 * Fills the line with spaces up to the caret in virtual space and [columnsAfterCaret] columns after it, and moves the
 * caret to the same column, now inside the text
 */
fun VimCaret.fillVirtualSpace(columnsAfterCaret: Int): VimCaret {
  if (!isInVirtualSpace) return this
  val columns = virtualColumns
  val padding = columns + columnsAfterCaret
  if (padding <= 0) return this
  val lineEndOffset = offset
  injector.application.runWriteAction {
    (editor as MutableVimEditor).insertText(this, lineEndOffset, " ".repeat(padding))
  }
  return moveToOffset(lineEndOffset + columns)
}

/**
 * Backspacing in virtual space only moves the caret back, there's no text to delete. `.` doesn't repeat the key: the
 * repeated insert has virtual space of its own, if any. `.` replays the keys once for all carets, the primary caret
 * decides.
 *
 * Moves the carets back here when all of them are in virtual space: the platform's backspace runs on the backend in
 * split mode, where the carets can't be after the line end. Returns false if the platform has to backspace.
 */
fun VimEditor.backspaceInVirtualSpace(): Boolean {
  if (primaryCaret().virtualColumns > 0) VimRedoBuffer.dropLastKey()
  if (carets().any { it.virtualColumns == 0 }) return false
  for (caret in carets()) {
    caret.moveToMotion(injector.motion.getHorizontalMotion(this, caret, -1, allowPastEnd = true, allowWrap = false))
  }
  return true
}

/**
 * Deleting a word in virtual space only moves the caret to the line end, see [backspaceInVirtualSpace].
 * Returns false if the caret is not after the line end.
 */
fun VimCaret.deleteWordInVirtualSpace(): Boolean {
  if (virtualColumns == 0) return false
  if (this == editor.primaryCaret()) VimRedoBuffer.dropLastKey()
  moveToBufferPosition(editor.offsetToBufferPosition(offset))
  return true
}

fun VimEditor.updateVirtualSpace() {
  if (isVirtualEditActive()) enableVirtualSpace()
}

/**
 * Gives the editor its own virtual space setting back when 'virtualedit' is not active, and moves the carets back to the
 * text. Called when a command is finished rather than on mode changes: a command can pass through a mode without
 * 'virtualedit' on its way, e.g. `<C-R>=` in Insert mode, and the editor moves the carets as soon as the setting changes
 */
fun VimEditor.leaveInactiveVirtualSpace() {
  if (isVirtualEditActive() || !restoreVirtualSpace()) return
  val allowEnd = isEndAllowed && mode.selectionType == null
  for (caret in carets()) {
    val offset = normalizeOffset(caret.offset, allowEnd)
    if (offset != caret.offset || caret.virtualColumns > 0) caret.moveToOffset(offset)
  }
}

/**
 * The motion to the [lineEndOffset] for `$` and `<End>`. With 'virtualedit', an operator from the line end or after it
 * has nothing to act on. Outside Insert mode, vertical motions then keep the column of the line end instead of sticking
 * to the end of lines.
 */
fun VimEditor.lineEndMotion(caret: ImmutableVimCaret, lineEndOffset: Int, count: Int, isForOperator: Boolean): Motion {
  if (isForOperator && count == 1 && caret.isInVirtualSpace) return Motion.AbsoluteOffset(caret.offset)
  if (isVirtualEditActive() && !inVisualMode && mode !is Mode.INSERT && mode != Mode.REPLACE) {
    return Motion.AbsoluteOffset(lineEndOffset)
  }
  return Motion.AdjustedOffset(lineEndOffset, VimMotionGroupBase.LAST_COLUMN)
}
