/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

/**
 * Vim defines three types of motions. These types mostly affect the behaviour of `d` command and friends.
 * The type of the motion can be found in vim documentation for this motion.
 */
enum class MotionType {
  INCLUSIVE,
  EXCLUSIVE,
  LINE_WISE,
}

enum class TextObjectVisualType {
  CHARACTER_WISE,
  LINE_WISE,
}

enum class CommandFlags {
  /**
   * Motion flags
   *
   * TODO it should be only INCLUSIVE, EXCLUSIVE and LINEWISE motions. Should be moved to [MotionType]
   */
  FLAG_MOT_LINEWISE,

  /**
   * Indicates that the cursor position should be saved prior to this motion command
   */
  FLAG_SAVE_JUMP,

  /**
   * A special command flag indicating that the inserted text after this command will not be repeated.
   * Example: `2i123` will insert `123123`, but `2s123` will insert `123`
   */
  FLAG_NO_REPEAT_INSERT,

  /**
   * This insert command should clear all saved keystrokes from the current insert
   */
  FLAG_CLEAR_STROKES,

  /**
   * This keystroke should be saved as part of the current insert
   */
  FLAG_SAVE_STROKE,

  /**
   * Don't include scrolljump when adjusting the scroll area to ensure the current cursor position is visible.
   * Should be used for commands that adjust the scroll area (such as <C-D> or <C-E>).
   * Technically, the current implementation doesn't need these flags, as these commands adjust the scroll area
   * according to their own rules and then move the cursor to fit (e.g. move cursor down a line with <C-E>). Moving the
   * cursor always tries to adjust the scroll area to ensure it's visible, which in this case is always a no-op.
   * This is an implementation detail, so keep the flags for both documentation and in case of refactoring.
   */
  FLAG_IGNORE_SCROLL_JUMP,
  FLAG_IGNORE_SIDE_SCROLL_JUMP,

  /**
   * This command should be followed by another command
   */
  FLAG_EXPECT_MORE,

  FLAG_START_EX,
  FLAG_END_EX,

  /**
   * TODO come up with a better solution
   */
  FLAG_UNDO_AWARE,
}
