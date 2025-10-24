/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.common.VimEditorReplaceMask
import com.maddyhome.idea.vim.helper.VimLockLabel
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Every line in [VimEditor] ends with a new line TODO <- this is probably not true already
 *
 * # New line and line count
 *
 * In vim **every** line always ends with a new line character. If you'll open a file that doesn't end with
 *   a new line and save it without a modification, the new line character will be automatically added.
 * This is not how the most editors work. The file and the line may end without a new character symbol.
 * This affects a [lineCount] public function. A three-lines file in vim contains four lines in other editors.
 * Also this complicated deletion logic.
 *
 * At the moment, we consider a what-user-sees approach. A file with two lines where every line ends with
 *   a new line character, is considered as a three-lines file because it's represented in the editor like this.
 *
 * TODO: We should understand what logic changes if we use a two or three lines editors.
 *
 * ---
 * # Lines
 *
 * We use two types of line reference: Offset and pointer. Offset referrers to a between-lines position.
 *   Pointer referrers to the concrete line.
 *
 * TODO We should check if we can keep the same VimEditor instance between edits.
 *   For example, can we store local options right in the editor implementation?
 *
 * ## Line kinds
 *
 * See `:help definitions`. Vim has the following types of lines:
 *
 * * "buffer lines" - the lines of the text buffer, as stored on disk. Equivalent to IntelliJ's `LogicalPosition`.
 *   Represented with [BufferPosition].
 * * "logical lines" - the lines of the text buffer, with folds applied. Folds in Vim are complete lines, but can be
 *   arbitrary ranges in IntelliJ, meaning a Vim logical line in IdeaVim can start and end on different buffer lines.
 * * "window lines" (sometimes referred to as "display" in the help) - the lines displayed in a window, including soft
 *   wrapped lines. These lines provide the character grid of the window, and the top left corner of the window is
 *   always `(0,0)`, regardless of the scroll state or wrap setting of the window's text. There is no representation in
 *   IdeaVim. IntelliJ's `VisualPosition` is similar, but is relative to the top of the text buffer, not the window. As
 *   such, IntelliJ does not have a direct equivalent. To avoid confusion with GUI windows, IdeaVim's API will use the
 *   word "display".
 * * "screen lines" - the lines of all windows, as well as status lines and the command line. IdeaVim does not support
 *   this and has no such representation.
 *
 * Unless otherwise noted, the vim-engine API will use buffer lines. Any parameter or variable called `line` will be a
 * buffer line. When required, Vim logical lines will use an appropriate type to represent a buffer start and end line,
 * and IntelliJ visual lines will be clearly marked in the parameter or variable name, e.g. `visualLine`.
 * ([VimVisualPosition] should be phased out if possible, as it is an IntelliJ concept, not a Vim concept.)
 */
interface VimEditor {
  var mode: Mode
  var isReplaceCharacter: Boolean
  val lfMakesNewLine: Boolean
  var vimChangeActionSwitchMode: Mode?
  val indentConfig: VimIndentConfig
  var replaceMask: VimEditorReplaceMask?

  fun fileSize(): Long

  /**
   * Return the text of the document
   *
   * This function should expect to be called multiple times, and therefore should not allocate and copy the entire text
   * of the document. For example, the search helpers call this function repeatedly.
   */
  fun text(): CharSequence

  /**
   * Vim has always at least one line. When we need to understand that there are no lines, it has a flag "ML_EMPTY"
   *   which indicated that the buffer is empty. However, the line count is still 1.
   *
   * The variable for line count is named `ml_line_count` in `memline` structure. There is a single spot where
   *   `0` is assigned to this variable (at the end of `buf_freeall` function), however I'm not sure that this affects
   *   the opened buffer.
   * Another thing that I don't understand is that I don't see where this variable is updated. There is a small chance
   *   that this variable doesn't present the line count, so I may be wrong and line count can return zero.
   * I've explored this question by looking at the implementation of ctrl-g command in normal mode.
   */
  fun lineCount(): Int {
    return nativeLineCount().coerceAtLeast(1)
  }

  fun nativeLineCount(): Int

  fun getLineRange(line: Int): Pair<Int, Int>
  fun carets(): List<VimCaret>
  fun sortedCarets(): List<VimCaret> = carets().sortedByOffset()
  fun nativeCarets(): List<VimCaret>
  fun sortedNativeCarets(): List<VimCaret> = nativeCarets().sortedByOffset()

  private fun List<VimCaret>.sortedByOffset(): List<VimCaret> {
    return this.sortedWith(compareBy { it.offset }).reversed()
  }

  /**
   * This method should perform caret merging after the operations. This is similar to IJ runForEachCaret
   * TODO review
   */

  fun forEachCaret(action: (VimCaret) -> Unit)
  fun forEachNativeCaret(action: (VimCaret) -> Unit, reverse: Boolean = false)
  fun isInForEachCaretScope(): Boolean

  // --------------------------------------------------------------------

  /**
   * Do we really need this?
   * TODO
   */
  fun primaryCaret(): VimCaret
  fun currentCaret(): VimCaret

  fun isWritable(): Boolean
  fun isDocumentWritable(): Boolean
  fun isOneLineMode(): Boolean

  /**
   * function for refactoring, get rid of it
   */
  fun search(
    pair: Pair<Int, Int>,
    editor: VimEditor,
    shiftType: LineDeleteShift,
  ): Pair<Pair<Int, Int>, LineDeleteShift>?

  fun offsetToBufferPosition(offset: Int): BufferPosition
  fun bufferPositionToOffset(position: BufferPosition): Int

  // TODO: [visual] Try to remove these. Visual position is an IntelliJ concept and doesn't have a Vim equivalent
  fun offsetToVisualPosition(offset: Int): VimVisualPosition
  fun visualPositionToOffset(position: VimVisualPosition): Int

  fun visualPositionToBufferPosition(position: VimVisualPosition): BufferPosition
  fun bufferPositionToVisualPosition(position: BufferPosition): VimVisualPosition

  fun bufferLineToVisualLine(line: Int): Int {
    return bufferPositionToVisualPosition(BufferPosition(line, 0)).line
  }

  fun getVirtualFile(): VimVirtualFile?
  @VimLockLabel.RequiresWriteLock
  fun deleteString(range: TextRange)

  fun getLineText(line: Int): String {
    val start: Int = getLineStartOffset(line)
    val end: Int = getLineEndOffset(line, true)
    return getText(start, end)
  }

  fun getScrollingModel(): VimScrollingModel

  fun removeCaret(caret: VimCaret)
  fun addCaret(offset: Int): VimCaret?
  fun removeSecondaryCarets()
  fun vimSetSystemBlockSelectionSilently(start: BufferPosition, end: BufferPosition)

  fun getLineStartOffset(line: Int): Int
  fun getLineEndOffset(line: Int): Int

  fun addCaretListener(listener: VimCaretListener)
  fun removeCaretListener(listener: VimCaretListener)

  fun isDisposed(): Boolean

  fun removeSelection()

  fun getPath(): String?
  fun extractProtocol(): String?

  // Can be used as a key to store something for specific project
  val projectId: String

  fun exitInsertMode(context: ExecutionContext)
  fun exitSelectModeNative(adjustCaret: Boolean)

  var vimLastSelectionType: SelectionType?

  fun isTemplateActive(): Boolean

  fun startGuardedBlockChecking()
  fun stopGuardedBlockChecking()

  fun hasUnsavedChanges(): Boolean

  fun getLastVisualLineColumnNumber(line: Int): Int

  fun createLiveMarker(start: Int, end: Int): LiveRange
  var insertMode: Boolean

  val document: VimDocument

  fun charAt(offset: Int): Char {
    return text()[offset]
  }

  fun createIndentBySize(size: Int): String
  fun getFoldRegionAtOffset(offset: Int): VimFoldRegion?

  /**
   * Mostly related to Fleet. After the editor is modified, the carets are modified. You can't use the old caret
   *   instance and need to search for a new version.
   */
  fun <T : ImmutableVimCaret> findLastVersionOfCaret(caret: T): T?

  fun resetOpPending() {
    if (this.mode is Mode.OP_PENDING) {
      mode = mode.returnTo
    }
  }

  /**
   * Toggles the insert/overwrite state. If currently insert, goto replace mode. If currently replace, goto insert
   * mode.
   */
  fun toggleInsertOverwrite() {
    val oldMode = this.mode
    var newMode = oldMode
    if (oldMode == Mode.INSERT) {
      newMode = Mode.REPLACE
    } else if (oldMode == Mode.REPLACE) {
      newMode = Mode.INSERT
    }
    if (oldMode != newMode) {
      mode = newMode
    }
  }
}

interface MutableVimEditor : VimEditor {
  fun addLine(atPosition: Int): Int?
  @VimLockLabel.RequiresWriteLock
  fun insertText(caret: VimCaret, atPosition: Int, text: CharSequence)
  @VimLockLabel.RequiresWriteLock
  fun replaceString(start: Int, end: Int, newString: String)
}

interface LinearEditor : VimEditor {
  fun getLine(offset: Int): Int
  fun getText(left: Int, right: Int): CharSequence
}

interface MutableLinearEditor : MutableVimEditor, LinearEditor {
  fun deleteRange(leftOffset: Int, rightOffset: Int)
}

enum class LineDeleteShift {
  NL_ON_START,
  NL_ON_END,
  NO_NL,
}

/**
 * Zero based line and column position within the text buffer.
 *
 * See `:help definitions` for terminology. Equivalent to IntelliJ's `LogicalPosition` which should not be confused for
 * Vim's "logical lines", which don't have an IntelliJ equivalent.
 */
class BufferPosition(
  val line: Int,
  val column: Int,
  val leansForward: Boolean = false,
) : Comparable<BufferPosition> {
  override fun compareTo(other: BufferPosition): Int {
    return if (line != other.line) line - other.line else column - other.column
  }
}

// TODO: [visual] Try to remove this. It's an IntelliJ concept and doesn't have a Vim equivalent
data class VimVisualPosition(val line: Int, val column: Int, val leansRight: Boolean = false)

interface VimFoldRegion {
  var isExpanded: Boolean
  val startOffset: Int
  val endOffset: Int
}
