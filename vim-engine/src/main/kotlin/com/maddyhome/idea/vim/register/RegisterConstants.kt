/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.register

object RegisterConstants {
  const val UNNAMED_REGISTER: Char = '"'
  const val LAST_SEARCH_REGISTER: Char = '/'
  const val LAST_COMMAND_REGISTER: Char = ':'
  const val LAST_INSERTED_TEXT_REGISTER: Char = '.'
  const val SMALL_DELETION_REGISTER: Char = '-'
  const val BLACK_HOLE_REGISTER: Char = '_'
  const val ALTERNATE_BUFFER_REGISTER: Char = '#'
  const val EXPRESSION_BUFFER_REGISTER: Char = '='
  const val CURRENT_FILENAME_REGISTER: Char = '%'

  const val CLIPBOARD_REGISTER: Char = '+'
  const val PRIMARY_REGISTER: Char = '*'
  const val CLIPBOARD_REGISTERS: String = CLIPBOARD_REGISTER.toString() + PRIMARY_REGISTER

  const val NUMBERED_REGISTERS: String = "0123456789"
  const val NAMED_REGISTERS: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

  const val WRITABLE_REGISTERS: String = (
    NUMBERED_REGISTERS + NAMED_REGISTERS + CLIPBOARD_REGISTERS +
      SMALL_DELETION_REGISTER + BLACK_HOLE_REGISTER + UNNAMED_REGISTER + LAST_SEARCH_REGISTER
    )

  const val READONLY_REGISTERS: String = (
    "" +
      CURRENT_FILENAME_REGISTER + LAST_COMMAND_REGISTER + LAST_INSERTED_TEXT_REGISTER + ALTERNATE_BUFFER_REGISTER +
      EXPRESSION_BUFFER_REGISTER
    ) // Expression buffer is not actually readonly

  const val RECORDABLE_REGISTERS: String = NUMBERED_REGISTERS + NAMED_REGISTERS + UNNAMED_REGISTER
  const val PLAYBACK_REGISTERS: String =
    RECORDABLE_REGISTERS + UNNAMED_REGISTER + CLIPBOARD_REGISTERS + LAST_INSERTED_TEXT_REGISTER
  const val VALID_REGISTERS: String = WRITABLE_REGISTERS + READONLY_REGISTERS
}
