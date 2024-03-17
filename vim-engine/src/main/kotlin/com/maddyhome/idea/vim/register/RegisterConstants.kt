/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.register

public object RegisterConstants {
  public const val UNNAMED_REGISTER: Char = '"'
  public const val LAST_SEARCH_REGISTER: Char = '/'
  public const val LAST_COMMAND_REGISTER: Char = ':'
  public const val LAST_INSERTED_TEXT_REGISTER: Char = '.'
  public const val SMALL_DELETION_REGISTER: Char = '-'
  public const val BLACK_HOLE_REGISTER: Char = '_'
  public const val ALTERNATE_BUFFER_REGISTER: Char = '#'
  public const val EXPRESSION_BUFFER_REGISTER: Char = '='
  public const val CURRENT_FILENAME_REGISTER: Char = '%'

  public const val CLIPBOARD_REGISTER: Char = '+'
  public const val PRIMARY_REGISTER: Char = '*'
  public const val CLIPBOARD_REGISTERS: String = CLIPBOARD_REGISTER.toString() + PRIMARY_REGISTER

  public const val NUMBERED_REGISTERS: String = "0123456789"
  public const val NAMED_REGISTERS: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

  public const val WRITABLE_REGISTERS: String = (
    NUMBERED_REGISTERS + NAMED_REGISTERS + CLIPBOARD_REGISTERS +
      SMALL_DELETION_REGISTER + BLACK_HOLE_REGISTER + UNNAMED_REGISTER + LAST_SEARCH_REGISTER
    )

  public const val READONLY_REGISTERS: String = (
    "" +
      CURRENT_FILENAME_REGISTER + LAST_COMMAND_REGISTER + LAST_INSERTED_TEXT_REGISTER + ALTERNATE_BUFFER_REGISTER +
      EXPRESSION_BUFFER_REGISTER
    ) // Expression buffer is not actually readonly

  public const val RECORDABLE_REGISTERS: String = NUMBERED_REGISTERS + NAMED_REGISTERS + UNNAMED_REGISTER
  public const val PLAYBACK_REGISTERS: String =
    RECORDABLE_REGISTERS + UNNAMED_REGISTER + CLIPBOARD_REGISTERS + LAST_INSERTED_TEXT_REGISTER
  public const val VALID_REGISTERS: String = WRITABLE_REGISTERS + READONLY_REGISTERS
}
