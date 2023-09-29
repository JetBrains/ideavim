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
annotation class CommandOrMotion(val keys: Array<String>, vararg val modes: Mode)

annotation class TextObject(val keys: String)


/**
 * @author vlan
 *
 * COMPATIBILITY-LAYER: Do not move this class to a different package
 */
enum class Mode {
  /**
   * Indicates this key mapping applies to Normal mode
   */
  NORMAL,

  /**
   * Indicates this key mapping applies to Visual mode
   */
  VISUAL,

  /**
   * Indicates this key mapping applies to Select mode
   */
  SELECT,

  /**
   * Indicates this key mapping applies to Operator Pending mode
   */
  OP_PENDING,

  /**
   * Indicates this key mapping applies to Insert mode
   */
  INSERT,

  /**
   * Indicates this key mapping applies to Command Line mode
   */
  CMD_LINE,
}