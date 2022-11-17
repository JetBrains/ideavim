/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.vfs.VirtualFileManager
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.MutableLinearEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretListener
import com.maddyhome.idea.vim.api.VimDocument
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLogicalPosition
import com.maddyhome.idea.vim.api.VimSelectionModel
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.VirtualFile
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.group.visual.vimSetSystemBlockSelectionSilently
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.exitInsertMode
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.isTemplateActive
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.updateCaretsVisualPosition
import com.maddyhome.idea.vim.helper.vimChangeActionSwitchMode
import com.maddyhome.idea.vim.helper.vimKeepingVisualOperatorAction
import com.maddyhome.idea.vim.helper.vimLastSelectionType
import com.maddyhome.idea.vim.options.helpers.StrictMode
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class IjVimEditor(editor: Editor) : MutableLinearEditor() {

  // All the editor actions should be performed with top level editor!!!
  // Be careful: all the EditorActionHandler implementation should correctly process InjectedEditors
  // TBH, I don't like the names. Need to think a bit more about this
  val editor = editor.getTopLevelEditor()
  val originalEditor = editor

  override val lfMakesNewLine: Boolean = true
  override var vimChangeActionSwitchMode: VimStateMachine.Mode?
    get() = editor.vimChangeActionSwitchMode
    set(value) {
      editor.vimChangeActionSwitchMode = value
    }
  override var vimKeepingVisualOperatorAction: Boolean
    get() = editor.vimKeepingVisualOperatorAction
    set(value) {
      editor.vimKeepingVisualOperatorAction = value
    }

  override fun fileSize(): Long = editor.fileSize.toLong()

  override fun text(): CharSequence {
    return editor.document.charsSequence
  }

  override fun nativeLineCount(): Int {
    return editor.document.lineCount
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
    forEachCaret(action, false)
  }

  override fun forEachCaret(action: (VimCaret) -> Unit, reverse: Boolean) {
    if (editor.inBlockSubMode) {
      action(IjVimCaret(editor.caretModel.primaryCaret))
    } else {
      editor.caretModel.runForEachCaret({ action(IjVimCaret(it)) }, reverse)
    }
  }

  override fun forEachNativeCaret(action: (VimCaret) -> Unit) {
    forEachNativeCaret(action, false)
  }

  override fun forEachNativeCaret(action: (VimCaret) -> Unit, reverse: Boolean) {
    editor.caretModel.runForEachCaret({ action(IjVimCaret(it)) }, reverse)
  }

  override fun primaryCaret(): VimCaret {
    return IjVimCaret(editor.caretModel.primaryCaret)
  }

  override fun currentCaret(): VimCaret {
    return IjVimCaret(editor.caretModel.currentCaret)
  }

  override fun isWritable(): Boolean {
    val modificationAllowed = EditorModificationUtil.checkModificationAllowed(editor)
    val writeRequested = EditorModificationUtil.requestWriting(editor)
    return modificationAllowed && writeRequested
  }

  override fun isDocumentWritable(): Boolean {
    return editor.document.isWritable
  }

  override fun isOneLineMode(): Boolean {
    return editor.isOneLineMode
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

  override fun offsetToVisualPosition(offset: Int): VimVisualPosition {
    return editor.offsetToVisualPosition(offset).let { VimVisualPosition(it.line, it.column, it.leansRight) }
  }

  override fun offsetToLogicalPosition(offset: Int): VimLogicalPosition {
    return editor.offsetToLogicalPosition(offset).let { VimLogicalPosition(it.line, it.column, it.leansForward) }
  }

  override fun logicalPositionToOffset(position: VimLogicalPosition): Int {
    val logicalPosition = LogicalPosition(position.line, position.column, position.leansForward)
    return editor.logicalPositionToOffset(logicalPosition)
  }

  override fun getVirtualFile(): VirtualFile? {
    val vf = EditorHelper.getVirtualFile(editor)
    return vf?.let {
      object : VirtualFile {
        override val path = vf.path
      }
    }
  }

  override fun deleteString(range: TextRange) {
    editor.document.deleteString(range.startOffset, range.endOffset)
  }

  override fun getSelectionModel(): VimSelectionModel {
    return object : VimSelectionModel {
      private val sm = editor.selectionModel
      override val selectionStart = sm.selectionStart
      override val selectionEnd = sm.selectionEnd

      override fun hasSelection(): Boolean {
        return sm.hasSelection()
      }
    }
  }

  override fun removeCaret(caret: VimCaret) {
    editor.caretModel.removeCaret((caret as IjVimCaret).caret)
  }

  override fun removeSecondaryCarets() {
    editor.caretModel.removeSecondaryCarets()
  }

  override fun vimSetSystemBlockSelectionSilently(start: VimLogicalPosition, end: VimLogicalPosition) {
    val startPosition = LogicalPosition(start.line, start.column, start.leansForward)
    val endPosition = LogicalPosition(end.line, end.column, end.leansForward)
    editor.selectionModel.vimSetSystemBlockSelectionSilently(startPosition, endPosition)
  }

  override fun getLineStartOffset(line: Int): Int {
    return if (line < 0) {
      StrictMode.fail("Incorrect line: $line")
      0
    } else if (line >= this.lineCount()) {
      if (lineCount() != 0) {
        StrictMode.fail("Incorrect line: $line")
      }
      editor.fileSize
    } else {
      editor.document.getLineStartOffset(line)
    }
  }

  override fun getLineEndOffset(line: Int): Int {
    return editor.document.getLineEndOffset(line)
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

  override fun exitInsertMode(context: ExecutionContext, operatorArguments: OperatorArguments) {
    editor.exitInsertMode(context.ij, operatorArguments)
  }

  override fun exitSelectModeNative(adjustCaret: Boolean) {
    this.exitSelectMode(adjustCaret)
  }

  override fun exitVisualModeNative() {
    this.editor.exitVisualMode()
  }

  override fun startGuardedBlockChecking() {
    val doc = editor.document
    doc.startGuardedBlockChecking()
  }

  override fun stopGuardedBlockChecking() {
    val doc = editor.document
    doc.stopGuardedBlockChecking()
  }

  override var vimLastSelectionType: SelectionType?
    get() = editor.vimLastSelectionType
    set(value) {
      editor.vimLastSelectionType = value
    }

  override fun isTemplateActive(): Boolean {
    return editor.isTemplateActive()
  }

  override fun hasUnsavedChanges(): Boolean {
    return EditorHelper.hasUnsavedChanges(this.editor)
  }

  override fun getLastVisualLineColumnNumber(line: Int): Int {
      return EditorUtil.getLastVisualLineColumnNumber(this.ij, line)
  }

  override fun visualToLogicalPosition(visualPosition: VimVisualPosition): VimLogicalPosition {
      val logPosition = editor.visualToLogicalPosition(
          VisualPosition(
              visualPosition.line,
              visualPosition.column,
              visualPosition.leansRight
          )
      )
      return VimLogicalPosition(logPosition.line, logPosition.column, logPosition.leansForward)
  }

  override fun logicalToVisualPosition(logicalPosition: VimLogicalPosition): VimVisualPosition {
    val visualPosition =
      editor.logicalToVisualPosition(logicalPosition.run { LogicalPosition(line, column, leansForward) })
    return visualPosition.run { VimVisualPosition(line, column, leansRight) }
  }

  override fun createLiveMarker(start: Offset, end: Offset): LiveRange {
    return editor.document.createRangeMarker(start.point, end.point).vim
  }

  /**
   * Converts a logical line number to a visual line number. Several logical lines can map to the same
   * visual line when there are collapsed fold regions.
   */
  override fun logicalLineToVisualLine(line: Int): Int {
    if (editor is EditorImpl) {
      // This is faster than simply calling Editor#logicalToVisualPosition
      return editor.offsetToVisualLine(editor.document.getLineStartOffset(line))
    }
    return super.logicalLineToVisualLine(line)
  }

  override var insertMode: Boolean
    get() = (editor as? EditorEx)?.isInsertMode ?: false
    set(value) {
      (editor as? EditorEx)?.isInsertMode = value
    }

  override val document: VimDocument
    get() = IjVimDocument(editor.document)

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
