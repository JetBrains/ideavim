/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.mark.Mark
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval

public interface VimMarkService {
  public companion object {
    public const val SELECTION_START_MARK: Char = '<'
    public const val SELECTION_END_MARK: Char = '>'

    public const val CHANGE_START_MARK: Char = '['
    public const val CHANGE_END_MARK: Char = ']'

    public const val PARAGRAPH_START_MARK: Char = '{'
    public const val PARAGRAPH_END_MARK: Char = '}'

    public const val SENTENCE_START_MARK: Char = '('
    public const val SENTENCE_END_MARK: Char = ')'

    public const val BEFORE_JUMP_MARK: Char = '\''
    public const val INSERT_EXIT_MARK: Char = '^'
    public const val LAST_BUFFER_POSITION: Char = '"'
    public const val LAST_CHANGE_MARK: Char = '.'

    public const val UPPERCASE_MARKS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    public const val LOWERCASE_MARKS: String = "abcdefghijklmnopqrstuvwxyz"
    public const val NUMBERED_MARKS: String = "0123456789"
  }

  /**
   * Get global mark
   */
  public fun getGlobalMark(char: Char): Mark?

  /**
   * Get mark for specified caret
   */
  public fun getMark(caret: ImmutableVimCaret, char: Char): Mark?

  /**
   * Gets all marks for caret
   */
  public fun getAllLocalMarks(caret: ImmutableVimCaret): Set<Mark>

  /**
   * Get all marks for specified filepath (for all carets in all editors)
   * @param editor  The editor with required file
   * @return list or pairs caret to mark (caret is null for global marks)
   */
  public fun getAllMarksForFile(editor: VimEditor): List<Pair<ImmutableVimCaret?, Set<Mark>>>

  /**
   * Gets all global marks
   */
  public fun getAllGlobalMarks(): Set<Mark>

  /**
   * Gets global marks for specified file
   */
  public fun getGlobalMarks(editor: VimEditor): Set<Mark>

  /**
   * Sets current caret position as mark for each caret in the editor
   *
   * @param editor  The editor to get the current position from
   * @param char    The mark to set
   * @return True if a valid, writable mark, false if not
   */
  public fun setMark(editor: VimEditor, char: Char): Boolean
  public fun setMark(caret: ImmutableVimCaret, mark: Mark): Boolean
  public fun setMark(caret: ImmutableVimCaret, char: Char, offset: Int): Boolean
  public fun setGlobalMark(editor: VimEditor, char: Char, offset: Int): Boolean
  public fun setGlobalMark(mark: Mark): Boolean

  public fun setVisualSelectionMarks(editor: VimEditor)
  public fun getVisualSelectionMarks(caret: ImmutableVimCaret): TextRange?

  public fun getChangeMarks(caret: ImmutableVimCaret): TextRange?

  /**
   * Sets the specified mark to the specified location
   *
   * @param caret   The caret for which the mark should be saved
   * @param char    The mark to set
   * @param offset  The offset to set the mark to
   * @return True if a valid, writable mark, false if not
   */
  public fun setMarkForCaret(caret: ImmutableVimCaret, char: Char, offset: Int): Boolean

  /**
   * Removes mark for all carets in the editor
   */
  public fun removeMark(editor: VimEditor, char: Char)

  /**
   * Removes mark for the given caret
   */
  public fun removeLocalMark(caret: ImmutableVimCaret, char: Char)

  // used to override it with IDE marks logic
  // or make it protected in base class?
  public fun removeGlobalMark(char: Char)

  /**
   * This updates all the marks for a file whenever text is inserted into the file. If the line that contains a mark
   * that is after the start of the insertion point, shift the mark by the number of new lines added.
   *
   * @param editor              The editor that was updated
   * @param insertStartOffset   The insertion point
   * @param insertLength        The length of the insertion
   */
  public fun updateMarksFromInsert(editor: VimEditor, insertStartOffset: Int, insertLength: Int)

  /**
   * This updates all the marks for a file whenever text is deleted from the file. If the line that contains a mark
   * is completely deleted then the mark is deleted too. If the deleted text is before the marked line, the mark is
   * moved up by the number of deleted lines.
   *
   * @param editor          The modified editor
   * @param delStartOffset  The offset within the editor where the deletion occurred
   * @param delLength       The length of the deleted text
   */
  public fun updateMarksFromDelete(editor: VimEditor, delStartOffset: Int, delLength: Int)

  public fun editorReleased(editor: VimEditor)

  public fun resetAllMarksForCaret(caret: ImmutableVimCaret)

  public fun resetAllMarks()

  public fun isValidMark(char: Char, operation: Operation, isCaretPrimary: Boolean): Boolean

  // Compatibility
  @Deprecated("Please use removeMark with other signature")
  @ScheduledForRemoval(inVersion = "2.3")
  public fun removeMark(ch: Char, mark: Mark)

  public enum class Operation {
    GET,
    SET,
    REMOVE,
    SAVE,
  }
}

public fun VimMarkService.setChangeMarks(caret: ImmutableVimCaret, range: TextRange) {
  setMark(caret, VimMarkService.CHANGE_START_MARK, range.startOffset)
  setMark(caret, VimMarkService.CHANGE_END_MARK, range.endOffset - 1)
}

public fun VimMarkService.setVisualSelectionMarks(caret: ImmutableVimCaret, range: TextRange) {
  setMark(caret, VimMarkService.SELECTION_START_MARK, range.startOffset)
  setMark(caret, VimMarkService.SELECTION_END_MARK, range.endOffset)
}
