/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.commands.SortOption
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

interface VimChangeGroup {
  fun setInsertRepeat(lines: Int, column: Int, append: Boolean)

  fun insertBeforeCaret(editor: VimEditor, context: ExecutionContext)

  fun insertBeforeFirstNonBlank(editor: VimEditor, context: ExecutionContext)

  fun insertLineStart(editor: VimEditor, context: ExecutionContext)

  fun insertAfterCaret(editor: VimEditor, context: ExecutionContext)

  fun insertAfterLineEnd(editor: VimEditor, context: ExecutionContext)

  fun insertPreviousInsert(
    editor: VimEditor,
    context: ExecutionContext,
    exit: Boolean,
    operatorArguments: OperatorArguments,
  )

  fun initInsert(editor: VimEditor, context: ExecutionContext, mode: Mode)

  /**
   * Enter Insert mode for block selection.
   *
   * Given a [TextRange] representing a block selection, position the primary caret either at the start column of the
   * selection for insert, or the end of the first line for append. Then set the insert repeat counts for the extent of
   * the block selection and start Insert mode.
   *
   * @param editor The Vim editor instance.
   * @param context The execution context.
   * @param range The range of text representing the block selection.
   * @param append Whether to insert before the range, or append after it.
   * @return True if the block was successfully inserted, false otherwise.
   */
  fun initBlockInsert(editor: VimEditor, context: ExecutionContext, range: TextRange, append: Boolean): Boolean

  fun processEscape(editor: VimEditor, context: ExecutionContext?)

  fun processEnter(editor: VimEditor, caret: VimCaret, context: ExecutionContext)
  fun processEnter(editor: VimEditor, context: ExecutionContext)
  fun processBackspace(editor: VimEditor, context: ExecutionContext)

  fun processPostChangeModeSwitch(editor: VimEditor, context: ExecutionContext, toSwitch: Mode)

  fun processCommand(editor: VimEditor, cmd: Command)

  fun deleteCharacter(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun processSingleCommand(editor: VimEditor)

  fun deleteEndOfLine(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun deleteJoinLines(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    spaces: Boolean,
  ): Boolean

  fun processKey(
    editor: VimEditor,
    key: KeyStroke,
    processResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean

  fun processKeyInSelectMode(
    editor: VimEditor,
    key: KeyStroke,
    processResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean
  fun deleteLine(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun deleteJoinRange(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    range: TextRange,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun joinViaIdeaByCount(editor: VimEditor, context: ExecutionContext, count: Int): Boolean

  fun joinViaIdeaBySelections(
    editor: VimEditor,
    context: ExecutionContext,
    caretsAndSelections: Map<VimCaret, VimSelection>,
  )

  fun getDeleteRangeAndType(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Pair<TextRange, SelectionType>?

  fun deleteRange(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType?,
    isChange: Boolean,
    saveToRegister: Boolean = true,
  ): Boolean

  fun changeCharacters(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun changeEndOfLine(
    editor: VimEditor,
    context: ExecutionContext,
    caret: VimCaret,
    count: Int,
    operatorArguments: OperatorArguments,
  ): Boolean

  /**
   * Delete the text covered by the motion command argument and enter insert mode
   *
   * @param editor   The editor to change
   * @param caret    The caret on which the motion is supposed to be performed
   * @param context  The data context
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  fun changeMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun changeCaseToggleCharacter(editor: VimEditor, caret: VimCaret, count: Int): Boolean

  fun changeCaseRange(editor: VimEditor, caret: VimCaret, range: TextRange, type: ChangeCaseType): Boolean

  fun changeRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType,
    context: ExecutionContext,
  ): Boolean

  fun changeCaseMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext?,
    type: ChangeCaseType,
    argument: Argument.Motion,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun reformatCodeMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun reformatCodeSelection(editor: VimEditor, caret: VimCaret, range: VimSelection)

  fun reformatCodeMotionPreserveCursor(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): Boolean

  fun reformatCodeSelectionPreserveCursor(editor: VimEditor, caret: VimCaret, range: VimSelection)

  fun autoIndentMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  )

  fun autoIndentRange(editor: VimEditor, caret: VimCaret, context: ExecutionContext, range: TextRange)

  fun indentLines(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    lines: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  )

  fun insertText(editor: VimEditor, caret: VimCaret, offset: Int, str: String): VimCaret

  fun insertText(editor: VimEditor, caret: VimCaret, str: String): VimCaret

  fun indentMotion(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    dir: Int,
    operatorArguments: OperatorArguments,
  )

  fun indentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
    count: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  )

  fun changeNumberVisualMode(
    editor: VimEditor,
    caret: VimCaret,
    selectedRange: TextRange,
    count: Int,
    avalanche: Boolean,
  ): Boolean

  fun changeNumber(editor: VimEditor, caret: VimCaret, count: Int): Boolean

  fun sortRange(
    editor: VimEditor,
    caret: VimCaret,
    range: LineRange,
    lineComparator: Comparator<String>,
    sortOptions: SortOption,
  ): Boolean

  fun reset()

  fun saveStrokes(newStrokes: String?)

  @TestOnly
  fun resetRepeat()
  fun runEnterAction(editor: VimEditor, context: ExecutionContext)
  fun runEnterAboveAction(editor: VimEditor, context: ExecutionContext)

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  fun repeatInsert(
    editor: VimEditor,
    context: ExecutionContext,
    count: Int,
    started: Boolean,
  )

  fun type(vimEditor: VimEditor, context: ExecutionContext, key: Char)
  fun type(vimEditor: VimEditor, context: ExecutionContext, string: String)
  fun replaceText(editor: VimEditor, caret: VimCaret, start: Int, end: Int, str: String)

  enum class ChangeCaseType {
    LOWER,
    UPPER,
    TOGGLE,
  }
}
