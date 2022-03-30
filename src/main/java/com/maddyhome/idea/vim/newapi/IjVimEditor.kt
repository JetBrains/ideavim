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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.MutableLinearEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretListener
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLogicalPosition
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.Pointer
import com.maddyhome.idea.vim.common.VimScrollType
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.group.visual.vimSetSystemBlockSelectionSilently
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.updateCaretsVisualPosition
import com.maddyhome.idea.vim.helper.vimLastSelectionType

class IjVimEditor(editor: Editor) : MutableLinearEditor() {

  // All the editor actions should be performed with top level editor!!!
  // Be careful: all the EditorActionHandler implementation should correctly process InjectedEditors
  // TBH, I don't like the names. Need to think a bit more about this
  val editor = editor.getTopLevelEditor()
  val originalEditor = editor

  override val lfMakesNewLine: Boolean = true

  override fun fileSize(): Long = editor.fileSize.toLong()

  override fun text(): CharSequence {
    return editor.document.charsSequence
  }

  override fun lineCount(): Int {
    val lineCount = editor.document.lineCount
    return lineCount.coerceAtLeast(1)
  }

  override fun deleteRange(leftOffset: Offset, rightOffset: Offset) {
    editor.document.deleteString(leftOffset.point, rightOffset.point)
  }

  override fun addLine(atPosition: EditorLine.Offset): EditorLine.Pointer {
    val offset: Int = if (atPosition.line < lineCount()) {

      // The new line character is inserted before the new line char of the previous line. So it works line an enter
      //   on a line end. I believe that the correct implementation would be to insert the new line char after the
      //   \n of the previous line, however at the moment this won't update the mark on this line.
      //   https://youtrack.jetbrains.com/issue/IDEA-286587

      val lineStart = (editor.document.getLineStartOffset(atPosition.line) - 1).coerceAtLeast(0)
      val guard = editor.document.getOffsetGuard(lineStart)
      if (guard != null && guard.endOffset == lineStart + 1) {
        // Dancing around guarded blocks. It may happen that this concrete position is locked, but the next
        //   (after the new line character) is not. In this case we can actually insert the line after this
        //   new line char
        // Such thing is often used in pycharm notebooks.
        lineStart + 1
      } else {
        lineStart
      }
    } else {
      fileSize().toInt()
    }
    editor.document.insertString(offset, "\n")
    return EditorLine.Pointer.init(atPosition.line, this)
  }

  override fun insertText(atPosition: Offset, text: CharSequence) {
    editor.document.insertString(atPosition.point, text)
  }

  // TODO: 30.12.2021 Is end offset inclusive?
  override fun getLineRange(line: EditorLine.Pointer): Pair<Offset, Offset> {
    // TODO: 30.12.2021 getLineEndOffset returns the same value for "xyz" and "xyz\n"
    return editor.document.getLineStartOffset(line.line).offset to editor.document.getLineEndOffset(line.line).offset
  }

  override fun getLine(offset: Offset): EditorLine.Pointer {
    return EditorLine.Pointer.init(editor.offsetToLogicalPosition(offset.point).line, this)
  }

  override fun charAt(offset: Pointer): Char {
    return editor.document.charsSequence[offset.point]
  }

  override fun carets(): List<VimCaret> {
    return if (editor.inBlockSubMode) {
      listOf(IjVimCaret(editor.caretModel.primaryCaret))
    } else {
      editor.caretModel.allCarets.map { IjVimCaret(it) }
    }
  }

  override fun nativeCarets(): List<VimCaret> {
    return editor.caretModel.allCarets.map { IjVimCaret(it) }
  }

  @Suppress("ideavimRunForEachCaret")
  override fun forEachCaret(action: (VimCaret) -> Unit) {
    if (editor.inBlockSubMode) {
      action(IjVimCaret(editor.caretModel.primaryCaret))
    } else {
      editor.caretModel.runForEachCaret { action(IjVimCaret(it)) }
    }
  }

  override fun primaryCaret(): VimCaret {
    return IjVimCaret(editor.caretModel.primaryCaret)
  }

  override fun isWritable(): Boolean {
    val modificationAllowed = EditorModificationUtil.checkModificationAllowed(editor)
    val writeRequested = EditorModificationUtil.requestWriting(editor)
    return modificationAllowed && writeRequested
  }

  override fun getText(left: Offset, right: Offset): CharSequence {
    return editor.document.charsSequence.subSequence(left.point, right.point)
  }

  override fun search(pair: Pair<Offset, Offset>, editor: VimEditor, shiftType: LineDeleteShift): Pair<Pair<Offset, Offset>, LineDeleteShift>? {
    val ijEditor = (editor as IjVimEditor).editor
    return when (shiftType) {
      LineDeleteShift.NO_NL -> if (pair.noGuard(ijEditor)) return pair to shiftType else null
      LineDeleteShift.NL_ON_END -> {
        if (pair.noGuard(ijEditor)) return pair to shiftType

        pair.shift(-1, -1) {
          if (this.noGuard(ijEditor)) return this to LineDeleteShift.NL_ON_START
        }

        pair.shift(shiftEnd = -1) {
          if (this.noGuard(ijEditor)) return this to LineDeleteShift.NO_NL
        }

        null
      }
      LineDeleteShift.NL_ON_START -> {
        if (pair.noGuard(ijEditor)) return pair to shiftType

        pair.shift(shiftStart = 1) {
          if (this.noGuard(ijEditor)) return this to LineDeleteShift.NO_NL
        }

        null
      }
    }
  }

  override fun updateCaretsVisualAttributes() {
    editor.updateCaretsVisualAttributes()
  }

  override fun updateCaretsVisualPosition() {
    editor.updateCaretsVisualPosition()
  }

  override fun lineEndForOffset(offset: Int): Int {
    return EditorHelper.getLineEndForOffset(editor, offset)
  }

  override fun lineStartForOffset(offset: Int): Int {
    return EditorHelper.getLineStartForOffset(editor, offset)
  }

  override fun offsetToLogicalPosition(offset: Int): VimLogicalPosition {
    return editor.offsetToLogicalPosition(offset).let { VimLogicalPosition(it.line, it.column, it.leansForward) }
  }

  override fun logicalPositionToOffset(position: VimLogicalPosition): Int {
    val logicalPosition = LogicalPosition(position.line, position.column, position.leansForward)
    return editor.logicalPositionToOffset(logicalPosition)
  }

  override fun lineLength(line: Int): Int {
    return EditorHelper.getLineLength(editor, line)
  }

  override fun removeSecondaryCarets() {
    editor.caretModel.removeSecondaryCarets()
  }

  override fun vimSetSystemBlockSelectionSilently(start: VimLogicalPosition, end: VimLogicalPosition) {
    val startPosition = LogicalPosition(start.line, start.column, start.leansForward)
    val endPosition = LogicalPosition(end.line, end.column, end.leansForward)
    editor.selectionModel.vimSetSystemBlockSelectionSilently(startPosition, endPosition)
  }

  override fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return EditorHelper.getLineEndOffset(editor, line, allowEnd)
  }

  val listenersMap: MutableMap<VimCaretListener, CaretListener> = mutableMapOf()

  override fun addCaretListener(listener: VimCaretListener) {
    val caretListener = object : CaretListener {
      override fun caretRemoved(event: CaretEvent) {
        listener.caretRemoved(event.caret?.vim)
      }
    }
    listenersMap[listener] = caretListener
    editor.caretModel.addCaretListener(caretListener)
  }

  override fun removeCaretListener(listener: VimCaretListener) {
    val caretListener = listenersMap.remove(listener) ?: error("Existing listener expected")
    editor.caretModel.removeCaretListener(caretListener)
  }

  override fun isDisposed(): Boolean {
    return editor.isDisposed
  }

  override fun removeSelection() {
    editor.selectionModel.removeSelection()
  }

  override fun getPath(): String? {
    return EditorHelper.getVirtualFile(editor)?.path
  }

  override fun extractProtocol(): String? {
    return EditorHelper.getVirtualFile(editor)?.getUrl()?.let { VirtualFileManager.extractProtocol(it) }
  }

  override fun visualPositionToOffset(position: VimVisualPosition): Offset {
    return editor.visualPositionToOffset(VisualPosition(position.line, position.column, position.leansRight)).offset
  }

  override fun exitSelectModeNative(adjustCaret: Boolean) {
    this.exitSelectMode(adjustCaret)
  }

  override fun exitVisualModeNative() {
    this.editor.exitVisualMode()
  }

  override var vimLastSelectionType: SelectionType?
    get() = editor.vimLastSelectionType
    set(value) {
      editor.vimLastSelectionType = value
    }

  override fun scrollToCaret(type: VimScrollType) {
    val scrollType = when (type) {
      VimScrollType.RELATIVE -> ScrollType.RELATIVE
      VimScrollType.CENTER -> ScrollType.CENTER
      VimScrollType.MAKE_VISIBLE -> ScrollType.MAKE_VISIBLE
      VimScrollType.CENTER_UP -> ScrollType.CENTER_UP
      VimScrollType.CENTER_DOWN -> ScrollType.CENTER_DOWN
    }
    editor.scrollingModel.scrollToCaret(scrollType)
  }

  override fun isTemplateActive(): Boolean {
    return editor.isTemplateActive()
  }

  private fun Pair<Offset, Offset>.noGuard(editor: Editor): Boolean {
    return editor.document.getRangeGuard(this.first.point, this.second.point) == null
  }

  private inline fun Pair<Offset, Offset>.shift(
    shiftStart: Int = 0,
    shiftEnd: Int = 0,
    action: Pair<Offset, Offset>.() -> Unit,
  ) {
    val data =
      (this.first.point + shiftStart).coerceAtLeast(0).offset to (this.second.point + shiftEnd).coerceAtLeast(0).offset
    data.action()
  }
  override fun equals(other: Any?): Boolean {
    error("equals and hashCode should not be used with IjVimEditor")
  }

  override fun hashCode(): Int {
    error("equals and hashCode should not be used with IjVimEditor")
  }
}

val Editor.vim: IjVimEditor
  get() = IjVimEditor(this)
val VimEditor.ij: Editor
  get() = (this as IjVimEditor).editor
