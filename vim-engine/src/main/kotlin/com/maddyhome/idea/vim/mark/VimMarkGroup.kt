package com.maddyhome.idea.vim.mark

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.TextRange

interface VimMarkGroup {
  fun saveJumpLocation(editor: VimEditor)
  fun setChangeMarks(vimEditor: VimEditor, range: TextRange)
  fun addJump(editor: VimEditor, reset: Boolean)

  /**
   * Gets the requested mark for the editor
   *
   * @param editor The editor to get the mark for
   * @param ch     The desired mark
   * @return The requested mark if set, null if not set
   */
  fun getMark(editor: VimEditor, ch: Char): Mark?

  /**
   * Get the requested jump.
   *
   * @param count Postive for next jump (Ctrl-I), negative for previous jump (Ctrl-O).
   * @return The jump or null if out of range.
   */
  fun getJump(count: Int): Jump?
  fun createSystemMark(ch: Char, line: Int, col: Int, editor: VimEditor): Mark?

  /**
   * Sets the specified mark to the specified location.
   *
   * @param editor  The editor the mark is associated with
   * @param ch      The mark to set
   * @param offset  The offset to set the mark to
   * @return true if able to set the mark, false if not
   */
  fun setMark(editor: VimEditor, ch: Char, offset: Int): Boolean

  /**
   * Sets the specified mark to the caret position of the editor
   *
   * @param editor  The editor to get the current position from
   * @param ch      The mark set set
   * @return True if a valid, writable mark, false if not
   */
  fun setMark(editor: VimEditor, ch: Char): Boolean
    fun includeCurrentCommandAsNavigation(editor: VimEditor)

  /**
   * Get's a mark from the file
   *
   * @param editor The editor to get the mark from
   * @param ch     The mark to get
   * @return The mark in the current file, if set, null if no such mark
   */
  fun getFileMark(editor: VimEditor, ch: Char): Mark?
  fun setVisualSelectionMarks(editor: VimEditor, range: TextRange)
  fun getChangeMarks(editor: VimEditor): TextRange?
  fun getVisualSelectionMarks(editor: VimEditor): TextRange?
  fun resetAllMarks()
  fun removeMark(ch: Char, mark: Mark)
  fun getMarks(editor: VimEditor): List<Mark>
  fun getJumps(): List<Jump>
  fun getJumpSpot(): Int
}
