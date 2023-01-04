/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.group.visual.VimSelection
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

interface VimChangeGroup {
  fun setInsertRepeat(lines: Int, column: Int, append: Boolean)

  fun insertBeforeCursor(editor: VimEditor, context: ExecutionContext)

  fun insertBeforeFirstNonBlank(editor: VimEditor, context: ExecutionContext)

  fun insertLineStart(editor: VimEditor, context: ExecutionContext)

  fun insertAfterCursor(editor: VimEditor, context: ExecutionContext)

  fun insertAfterLineEnd(editor: VimEditor, context: ExecutionContext)

  fun insertPreviousInsert(editor: VimEditor, context: ExecutionContext, exit: Boolean, operatorArguments: OperatorArguments)

  fun insertLineAround(editor: VimEditor, context: ExecutionContext, shift: Int)

  fun initInsert(editor: VimEditor, context: ExecutionContext, mode: VimStateMachine.Mode)

  fun processEscape(editor: VimEditor, context: ExecutionContext?, operatorArguments: OperatorArguments)

  fun processEnter(editor: VimEditor, context: ExecutionContext)

  fun processPostChangeModeSwitch(editor: VimEditor, context: ExecutionContext, toSwitch: VimStateMachine.Mode)

  fun processCommand(editor: VimEditor, cmd: Command)

  fun deleteCharacter(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    isChange: Boolean,
    operatorArguments: OperatorArguments
  ): Boolean

  fun processSingleCommand(editor: VimEditor)

  fun deleteEndOfLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean

  fun deleteJoinLines(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    spaces: Boolean,
    operatorArguments: OperatorArguments
  ): Boolean

  fun processKey(editor: VimEditor, context: ExecutionContext, key: KeyStroke): Boolean

  fun processKeyInSelectMode(editor: VimEditor, context: ExecutionContext, key: KeyStroke): Boolean

  fun deleteLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean

  fun deleteJoinRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    spaces: Boolean,
    operatorArguments: OperatorArguments
  ): Boolean

  fun joinViaIdeaByCount(editor: VimEditor, context: ExecutionContext, count: Int): Boolean

  fun joinViaIdeaBySelections(editor: VimEditor, context: ExecutionContext, caretsAndSelections: Map<VimCaret, VimSelection>)

  fun getDeleteRangeAndType(editor: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext, argument: Argument, isChange: Boolean, operatorArguments: OperatorArguments): Pair<TextRange, SelectionType>?

  fun getDeleteRangeAndType2(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, isChange: Boolean, operatorArguments: OperatorArguments): Pair<TextRange, SelectionType>?

  fun deleteRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType?,
    isChange: Boolean,
    operatorArguments: OperatorArguments
  ): Boolean
  fun deleteRange2(editor: VimEditor, caret: VimCaret, range: TextRange, type: SelectionType): Boolean

  fun changeCharacters(editor: VimEditor, caret: VimCaret, operatorArguments: OperatorArguments): Boolean

  fun changeEndOfLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean

  /**
   * Delete the text covered by the motion command argument and enter insert mode
   *
   * @param editor   The editor to change
   * @param caret    The caret on which the motion is supposed to be performed
   * @param context  The data context
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  fun changeMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, operatorArguments: OperatorArguments): Boolean

  fun changeCaseToggleCharacter(editor: VimEditor, caret: VimCaret, count: Int): Boolean

  fun blockInsert(editor: VimEditor, context: ExecutionContext, range: TextRange, append: Boolean, operatorArguments: OperatorArguments): Boolean

  fun changeCaseRange(editor: VimEditor, caret: VimCaret, range: TextRange, type: Char): Boolean

  fun changeRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType,
    context: ExecutionContext,
    operatorArguments: OperatorArguments
  ): Boolean

  fun changeCaseMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext?, type: Char, argument: Argument, operatorArguments: OperatorArguments): Boolean

  fun reformatCodeMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext?, argument: Argument, operatorArguments: OperatorArguments): Boolean

  fun reformatCodeSelection(editor: VimEditor, caret: VimCaret, range: VimSelection)

  fun autoIndentMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, operatorArguments: OperatorArguments)

  fun autoIndentRange(editor: VimEditor, caret: VimCaret, context: ExecutionContext, range: TextRange)

  fun indentLines(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    lines: Int,
    dir: Int,
    operatorArguments: OperatorArguments
  )

  fun insertText(editor: VimEditor, caret: VimCaret, offset: Int, str: String): VimCaret

  fun insertText(editor: VimEditor, caret: VimCaret, str: String): VimCaret

  fun indentMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, dir: Int, operatorArguments: OperatorArguments)

  fun indentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
    count: Int,
    dir: Int,
    operatorArguments: OperatorArguments
  )

  fun changeNumberVisualMode(editor: VimEditor, caret: VimCaret, selectedRange: TextRange, count: Int, avalanche: Boolean): Boolean

  fun changeNumber(editor: VimEditor, caret: VimCaret, count: Int): Boolean

  fun sortRange(editor: VimEditor, range: LineRange, lineComparator: Comparator<String>): Boolean

  fun reset()

  fun saveStrokes(newStrokes: String?)

  @TestOnly
  fun resetRepeat()
  fun notifyListeners(editor: VimEditor)
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
    operatorArguments: OperatorArguments,
  )

  fun type(vimEditor: VimEditor, context: ExecutionContext, key: Char)
  fun replaceText(editor: VimEditor, start: Int, end: Int, str: String)
}
