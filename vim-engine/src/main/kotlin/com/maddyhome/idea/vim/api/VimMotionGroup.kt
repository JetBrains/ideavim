/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.motion.leftright.TillCharacterMotionType
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.Motion

public interface VimMotionGroup {

  // Note that the following methods require the caret to access the intended vertical position, such as "end of line"
  public fun getHorizontalMotion(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowPastEnd: Boolean, allowWrap: Boolean = false): Motion
  public fun getVerticalMotionOffset(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Motion

// TODO: Consider naming. These don't move the caret, but calculate offsets. Also consider returning Motion

  // Move caret to specific buffer line
  public fun moveCaretToLineStart(editor: VimEditor, line: Int): Int
  public fun moveCaretToLineStartSkipLeading(editor: VimEditor, line: Int): Int
  public fun moveCaretToLineWithStartOfLineOption(editor: VimEditor, line: Int, caret: ImmutableVimCaret): Int
  public fun moveCaretToLineEnd(editor: VimEditor, line: Int, allowPastEnd: Boolean): Int
  public fun moveCaretToLineEndSkipTrailing(editor: VimEditor, line: Int): Int
  public fun moveCaretToLineWithSameColumn(editor: VimEditor, line: Int, caret: ImmutableVimCaret): Int
  public fun moveCaretToLinePercent(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int

  // Move caret relative to current line
  public fun moveCaretToRelativeLineStartSkipLeading(editor: VimEditor, caret: ImmutableVimCaret, linesOffset: Int): Int
  public fun moveCaretToRelativeLineEnd(editor: VimEditor, caret: ImmutableVimCaret, cntForward: Int, allowPastEnd: Boolean): Int
  public fun moveCaretToRelativeLineEndSkipTrailing(editor: VimEditor, caret: ImmutableVimCaret, linesOffset: Int): Int

  // Move caret to (IntelliJ visual) line relative to the bounds of the display (aka window)
  // (This describes what these public functions *currently* do, not what they are *supposed* to do)
  // TODO: These should move to the Vim logical line at the top/bottom/middle of the display
  public fun moveCaretToFirstDisplayLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, normalizeToScreen: Boolean): Int
  public fun moveCaretToMiddleDisplayLine(editor: VimEditor, caret: ImmutableVimCaret): Int
  public fun moveCaretToLastDisplayLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, normalizeToScreen: Boolean): Int

  // Move caret to buffer column
  public fun moveCaretToColumn(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowEnd: Boolean): Motion

  // Move caret to buffer column on current line
  public fun moveCaretToCurrentLineStart(editor: VimEditor, caret: ImmutableVimCaret): Int
  public fun moveCaretToCurrentLineStartSkipLeading(editor: VimEditor, caret: ImmutableVimCaret): Int
  public fun moveCaretToCurrentLineEnd(editor: VimEditor, caret: ImmutableVimCaret): Int

  // Move caret to column relative to the bounds of the display (aka window)
  public fun moveCaretToCurrentDisplayLineStart(editor: VimEditor, caret: ImmutableVimCaret): Motion
  public fun moveCaretToCurrentDisplayLineStartSkipLeading(editor: VimEditor, caret: ImmutableVimCaret): Int
  public fun moveCaretToCurrentDisplayLineMiddle(editor: VimEditor, caret: ImmutableVimCaret): Motion
  public fun moveCaretToCurrentDisplayLineEnd(editor: VimEditor, caret: ImmutableVimCaret, allowEnd: Boolean): Motion

  // Move caret to other
  public fun moveCaretToMark(caret: ImmutableVimCaret, ch: Char, toLineStart: Boolean): Motion
  public fun moveCaretToJump(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Motion
  public fun moveCaretToMatchingPair(editor: VimEditor, caret: ImmutableVimCaret): Motion

  /**
   * Find the offset of the start of the next/previous word/WORD
   *
   * @param editor      The editor to search in
   * @param searchFrom  The buffer offset to start searching from
   * @param count       The number of words to skip
   * @param bigWord     If true then find WORD, if false then find word
   * @return a [Motion] representing the offset to move to, or [Motion.Error] if not found
   */
  public fun findOffsetOfNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Motion

  // Next/previous matching character - f/F and t/T motions
  public val lastFTCmd: TillCharacterMotionType
  public val lastFTChar: Char
  public fun setLastFTCmd(lastFTCmd: TillCharacterMotionType, lastChar: Char)

  /**
   * Get the offset of the next/previous matching character on the caret's buffer line
   *
   * @param editor The editor to search in
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @return the buffer offset to move to, or -1 if not found
   */
  public fun moveCaretToNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int

  /**
   * Get the offset of the character preceding (in direction of travel) the next/previous matching character on the
   * caret's buffer line
   *
   * @param editor The editor to search in
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @return the buffer offset to move to, or -1 if not found
   */
  public fun moveCaretToBeforeNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int

  public fun repeatLastMatchChar(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int

  public fun getMotionRange(editor: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext, argument: Argument, operatorArguments: OperatorArguments): TextRange?

  // TODO: These aren't caret motions. Should be moved to VimWindowGroup?
  public fun moveCaretGotoNextTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int
  public fun moveCaretGotoPreviousTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int
  public fun onAppCodeMovement(editor: VimEditor, caret: VimCaret, offset: Int, oldOffset: Int)
}
