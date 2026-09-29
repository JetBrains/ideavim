/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.annotations

// TODO support numpad keys parsing, see :keycodes
/**
 * It's not necessary a Vim command
 * This annotation may be used for:
 * - commands
 * - motions
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CommandOrMotion(
  val keys: Array<String>,
  vararg val modes: Mode,
  /**
   * Registers this handler for the same keys as its normal counterpart, to be used only while a completion popup is
   * open.
   *
   * `<C-Y>` accepts the selected item with a popup open and inserts the character above the caret without one. Those
   * are two commands sharing a keystroke, so they are two handlers: `AcceptLookupAction` declares `lookup = true` and
   * `InsertCharacterAboveCursorAction` does not. Exactly one of each pair may exist for a given key and mode.
   */
  val lookup: Boolean = false,
)

annotation class TextObject(val keys: String)


enum class Mode(val abbrev: Char) {
  /**
   * Indicates this key mapping applies to Normal mode
   */
  NORMAL('N'),

  /**
   * Indicates this key mapping applies to Visual mode
   */
  VISUAL('X'),

  /**
   * Indicates this key mapping applies to Select mode
   */
  SELECT('S'),

  /**
   * Indicates this key mapping applies to Operator Pending mode
   */
  OP_PENDING('O'),

  /**
   * Indicates this key mapping applies to Insert or Replace modes
   */
  INSERT('I'),

  /**
   * Indicates this key mapping applies to Command Line mode
   */
  CMD_LINE('C'),
}