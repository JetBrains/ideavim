/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

object VimMarkConstants {
  const val MARK_VISUAL_START: Char = '<'
  const val MARK_VISUAL_END: Char = '>'
  const val MARK_CHANGE_START: Char = '['
  const val MARK_CHANGE_END: Char = ']'
  const val MARK_CHANGE_POS: Char = '.'

  const val WR_GLOBAL_MARKS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  const val WR_REGULAR_FILE_MARKS: String = "abcdefghijklmnopqrstuvwxyz"

  /** Marks: abcdefghijklmnopqrstuvwxyz'  */
  const val WR_FILE_MARKS: String = "$WR_REGULAR_FILE_MARKS'"

  const val RO_GLOBAL_MARKS: String = "0123456789"
  const val RO_FILE_MARKS: String = ".[]<>^{}()"

  const val DEL_CONTEXT_FILE_MARKS: String = ".^[]\""

  /** Marks: .^[]"abcdefghijklmnopqrstuvwxyz  */
  const val DEL_FILE_MARKS: String = DEL_CONTEXT_FILE_MARKS + WR_REGULAR_FILE_MARKS

  /** Marks: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ */
  const val DEL_GLOBAL_MARKS: String = RO_GLOBAL_MARKS + WR_GLOBAL_MARKS

  /** Marks: .^[]"abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ   */
  const val DEL_MARKS: String = DEL_FILE_MARKS + DEL_GLOBAL_MARKS

  /** Marks: abcdefghijklmnopqrstuvwxyz'.^[]"  */
  const val SAVE_FILE_MARKS: String = WR_FILE_MARKS + DEL_CONTEXT_FILE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789  */
  const val GLOBAL_MARKS: String = WR_GLOBAL_MARKS + RO_GLOBAL_MARKS

  /** Marks: abcdefghijklmnopqrstuvwxyz'[]<>^{}()  */
  const val FILE_MARKS: String = WR_FILE_MARKS + RO_FILE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'  */
  const val WRITE_MARKS: String = WR_GLOBAL_MARKS + WR_FILE_MARKS

  /** Marks: 0123456789.[]<>^{}()  */
  const val READONLY_MARKS: String = RO_GLOBAL_MARKS + RO_FILE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'  */
  const val VALID_SET_MARKS: String = WRITE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'0123456789.[]<>^{}()  */
  const val VALID_GET_MARKS: String = WRITE_MARKS + READONLY_MARKS
}
