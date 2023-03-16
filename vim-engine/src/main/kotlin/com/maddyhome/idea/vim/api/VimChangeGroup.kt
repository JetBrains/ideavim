/*
 * Copyright 2003-2023 The IdeaVim authors
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

public interface VimChangeGroup {
  public fun setInsertRepeat(lines: Int, column: Int, append: Boolean)

  public fun insertBeforeCursor(editor: VimEditor, context: ExecutionContext)

  public fun insertBeforeFirstNonBlank(editor: VimEditor, context: ExecutionContext)

  public fun insertLineStart(editor: VimEditor, context: ExecutionContext)

  public fun insertAfterCursor(editor: VimEditor, context: ExecutionContext)

  public fun insertAfterLineEnd(editor: VimEditor, context: ExecutionContext)

  public fun insertPreviousInsert(editor: VimEditor, context: ExecutionContext, exit: Boolean, operatorArguments: OperatorArguments)

  public fun initInsert(editor: VimEditor, context: ExecutionContext, mode: VimStateMachine.Mode)

  public fun processEscape(editor: VimEditor, context: ExecutionContext?, operatorArguments: OperatorArguments)

  public fun processEnter(editor: VimEditor, context: ExecutionContext)

  public fun processPostChangeModeSwitch(editor: VimEditor, context: ExecutionContext, toSwitch: VimStateMachine.Mode)

  public fun processCommand(editor: VimEditor, cmd: Command)

  public fun deleteCharacter(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean

  public fun processSingleCommand(editor: VimEditor)

  public fun deleteEndOfLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean

  public fun deleteJoinLines(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean

  public fun processKey(editor: VimEditor, context: ExecutionContext, key: KeyStroke): Boolean

  public fun processKeyInSelectMode(editor: VimEditor, context: ExecutionContext, key: KeyStroke): Boolean

  public fun deleteLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean

  public fun deleteJoinRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    spaces: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean

  public fun joinViaIdeaByCount(editor: VimEditor, context: ExecutionContext, count: Int): Boolean

  public fun joinViaIdeaBySelections(editor: VimEditor, context: ExecutionContext, caretsAndSelections: Map<VimCaret, VimSelection>)

  public fun getDeleteRangeAndType(editor: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext, argument: Argument, isChange: Boolean, operatorArguments: OperatorArguments): Pair<TextRange, SelectionType>?

  public fun getDeleteRangeAndType2(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, isChange: Boolean, operatorArguments: OperatorArguments): Pair<TextRange, SelectionType>?

  public fun deleteRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType?,
    isChange: Boolean,
    operatorArguments: OperatorArguments,
    saveToRegister: Boolean = true,
  ): Boolean
  public fun changeCharacters(editor: VimEditor, caret: VimCaret, operatorArguments: OperatorArguments): Boolean

  public fun changeEndOfLine(editor: VimEditor, caret: VimCaret, count: Int, operatorArguments: OperatorArguments): Boolean

  /**
   * Delete the text covered by the motion command argument and enter insert mode
   *
   * @param editor   The editor to change
   * @param caret    The caret on which the motion is supposed to be performed
   * @param context  The data context
   * @param argument The motion command
   * @return true if able to delete the text, false if not
   */
  public fun changeMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, operatorArguments: OperatorArguments): Boolean

  public fun changeCaseToggleCharacter(editor: VimEditor, caret: VimCaret, count: Int): Boolean

  public fun blockInsert(editor: VimEditor, context: ExecutionContext, range: TextRange, append: Boolean, operatorArguments: OperatorArguments): Boolean

  public fun changeCaseRange(editor: VimEditor, caret: VimCaret, range: TextRange, type: Char): Boolean

  public fun changeRange(
    editor: VimEditor,
    caret: VimCaret,
    range: TextRange,
    type: SelectionType,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): Boolean

  public fun changeCaseMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext?, type: Char, argument: Argument, operatorArguments: OperatorArguments): Boolean

  public fun reformatCodeMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext?, argument: Argument, operatorArguments: OperatorArguments): Boolean

  public fun reformatCodeSelection(editor: VimEditor, caret: VimCaret, range: VimSelection)

  public fun autoIndentMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, operatorArguments: OperatorArguments)

  public fun autoIndentRange(editor: VimEditor, caret: VimCaret, context: ExecutionContext, range: TextRange)

  public fun indentLines(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    lines: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  )

  public fun insertText(editor: VimEditor, caret: VimCaret, offset: Int, str: String): VimCaret

  public fun insertText(editor: VimEditor, caret: VimCaret, str: String): VimCaret

  public fun indentMotion(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, dir: Int, operatorArguments: OperatorArguments)

  public fun indentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
    count: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  )

  public fun changeNumberVisualMode(editor: VimEditor, caret: VimCaret, selectedRange: TextRange, count: Int, avalanche: Boolean): Boolean

  public fun changeNumber(editor: VimEditor, caret: VimCaret, count: Int): Boolean

  public fun sortRange(editor: VimEditor, caret: VimCaret, range: LineRange, lineComparator: Comparator<String>): Boolean

  public fun reset()

  public fun saveStrokes(newStrokes: String?)

  @TestOnly
  public fun resetRepeat()
  public fun notifyListeners(editor: VimEditor)
  public fun runEnterAction(editor: VimEditor, context: ExecutionContext)
  public fun runEnterAboveAction(editor: VimEditor, context: ExecutionContext)

  /**
   * This repeats the previous insert count times
   *
   * @param editor  The editor to insert into
   * @param context The data context
   * @param count   The number of times to repeat the previous insert
   */
  public fun repeatInsert(
    editor: VimEditor,
    context: ExecutionContext,
    count: Int,
    started: Boolean,
    operatorArguments: OperatorArguments,
  )

  public fun type(vimEditor: VimEditor, context: ExecutionContext, key: Char)
  public fun replaceText(editor: VimEditor, caret: VimCaret, start: Int, end: Int, str: String)
}
