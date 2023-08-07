/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimCaretListener
import com.maddyhome.idea.vim.api.VimDocument
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimScrollingModel
import com.maddyhome.idea.vim.api.VimSelectionModel
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.VirtualFile
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.TextRange

/**
 * Simplified implementation of the VimEditor interface used just for testing.
 * Currently only has a way to retrieve text. Need to mock other functionalities
 * like carets, marks, etc.
 */
internal class VimEditorMock(val text: CharSequence,
                                 override val lfMakesNewLine: Boolean = false,
                                 override var vimChangeActionSwitchMode: VimStateMachine.Mode? = null,
                                 override var vimKeepingVisualOperatorAction: Boolean = false,
                                 override var vimLastSelectionType: SelectionType? = null,
                                 override var insertMode: Boolean = false,
                                 override val document: VimDocument = VimDocumentMock()
) : VimEditor {

  override fun text(): CharSequence {
    return text
  }
  override fun fileSize(): Long {
    TODO("Not yet implemented")
  }

  override fun nativeLineCount(): Int {
    TODO("Not yet implemented")
  }

  override fun getLineRange(line: EditorLine.Pointer): Pair<Offset, Offset> {
    TODO("Not yet implemented")
  }

  override fun carets(): List<VimCaret> {
    TODO("Not yet implemented")
  }

  override fun nativeCarets(): List<VimCaret> {
    TODO("Not yet implemented")
  }

  override fun forEachCaret(action: (VimCaret) -> Unit) {
    TODO("Not yet implemented")
  }

  override fun forEachNativeCaret(action: (VimCaret) -> Unit, reverse: Boolean) {
    TODO("Not yet implemented")
  }

  override fun primaryCaret(): VimCaret {
    TODO("Not yet implemented")
  }

  override fun currentCaret(): VimCaret {
    TODO("Not yet implemented")
  }

  override fun isWritable(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isDocumentWritable(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isOneLineMode(): Boolean {
    TODO("Not yet implemented")
  }

  override fun search(
    pair: Pair<Offset, Offset>,
    editor: VimEditor,
    shiftType: LineDeleteShift,
  ): Pair<Pair<Offset, Offset>, LineDeleteShift>? {
    TODO("Not yet implemented")
  }

  override fun updateCaretsVisualAttributes() {
    TODO("Not yet implemented")
  }

  override fun updateCaretsVisualPosition() {
    TODO("Not yet implemented")
  }

  override fun offsetToBufferPosition(offset: Int): BufferPosition {
    TODO("Not yet implemented")
  }

  override fun bufferPositionToOffset(position: BufferPosition): Int {
    TODO("Not yet implemented")
  }

  override fun offsetToVisualPosition(offset: Int): VimVisualPosition {
    TODO("Not yet implemented")
  }

  override fun visualPositionToOffset(position: VimVisualPosition): Offset {
    TODO("Not yet implemented")
  }

  override fun visualPositionToBufferPosition(position: VimVisualPosition): BufferPosition {
    TODO("Not yet implemented")
  }

  override fun bufferPositionToVisualPosition(position: BufferPosition): VimVisualPosition {
    TODO("Not yet implemented")
  }

  override fun getVirtualFile(): VirtualFile? {
    TODO("Not yet implemented")
  }

  override fun deleteString(range: TextRange) {
    TODO("Not yet implemented")
  }

  override fun getSelectionModel(): VimSelectionModel {
    TODO("Not yet implemented")
  }

  override fun getScrollingModel(): VimScrollingModel {
    TODO("Not yet implemented")
  }

  override fun removeCaret(caret: VimCaret) {
    TODO("Not yet implemented")
  }

  override fun removeSecondaryCarets() {
    TODO("Not yet implemented")
  }

  override fun vimSetSystemBlockSelectionSilently(start: BufferPosition, end: BufferPosition) {
    TODO("Not yet implemented")
  }

  override fun getLineStartOffset(line: Int): Int {
    TODO("Not yet implemented")
  }

  override fun getLineEndOffset(line: Int): Int {
    TODO("Not yet implemented")
  }

  override fun addCaretListener(listener: VimCaretListener) {
    TODO("Not yet implemented")
  }

  override fun removeCaretListener(listener: VimCaretListener) {
    TODO("Not yet implemented")
  }

  override fun isDisposed(): Boolean {
    TODO("Not yet implemented")
  }

  override fun removeSelection() {
    TODO("Not yet implemented")
  }

  override fun getPath(): String? {
    TODO("Not yet implemented")
  }

  override fun extractProtocol(): String? {
    TODO("Not yet implemented")
  }

  override fun exitInsertMode(context: ExecutionContext, operatorArguments: OperatorArguments) {
    TODO("Not yet implemented")
  }

  override fun exitSelectModeNative(adjustCaret: Boolean) {
    TODO("Not yet implemented")
  }

  override fun isTemplateActive(): Boolean {
    TODO("Not yet implemented")
  }

  override fun startGuardedBlockChecking() {
    TODO("Not yet implemented")
  }

  override fun stopGuardedBlockChecking() {
    TODO("Not yet implemented")
  }

  override fun hasUnsavedChanges(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getLastVisualLineColumnNumber(line: Int): Int {
    TODO("Not yet implemented")
  }

  override fun createLiveMarker(start: Offset, end: Offset): LiveRange {
    TODO("Not yet implemented")
  }

  override fun createIndentBySize(size: Int): String {
    TODO("Not yet implemented")
  }

  override fun getCollapsedRegionAtOffset(offset: Int): TextRange? {
    TODO("Not yet implemented")
  }

  override fun <T : ImmutableVimCaret> findLastVersionOfCaret(caret: T): T? {
    TODO("Not yet implemented")
  }
}

private class VimDocumentMock : VimDocument {
  override fun addChangeListener(listener: ChangesListener) {
    TODO("Not yet implemented")
  }

  override fun removeChangeListener(listener: ChangesListener) {
    TODO("Not yet implemented")
  }

  override fun getOffsetGuard(offset: Offset): LiveRange? {
    TODO("Not yet implemented")
  }
}
