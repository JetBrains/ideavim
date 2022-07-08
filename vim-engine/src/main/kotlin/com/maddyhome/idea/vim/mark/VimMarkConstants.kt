package com.maddyhome.idea.vim.mark

object VimMarkConstants {
  const val MARK_VISUAL_START = '<'
  const val MARK_VISUAL_END = '>'
  const val MARK_CHANGE_START = '['
  const val MARK_CHANGE_END = ']'
  const val MARK_CHANGE_POS = '.'

  const val WR_GLOBAL_MARKS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  const val WR_REGULAR_FILE_MARKS = "abcdefghijklmnopqrstuvwxyz"

  /** Marks: abcdefghijklmnopqrstuvwxyz'  */
  const val WR_FILE_MARKS = "$WR_REGULAR_FILE_MARKS'"

  const val RO_GLOBAL_MARKS = "0123456789"
  const val RO_FILE_MARKS = ".[]<>^{}()"

  const val DEL_CONTEXT_FILE_MARKS = ".^[]\""

  /** Marks: .^[]"abcdefghijklmnopqrstuvwxyz  */
  const val DEL_FILE_MARKS = DEL_CONTEXT_FILE_MARKS + WR_REGULAR_FILE_MARKS

  /** Marks: 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ */
  const val DEL_GLOBAL_MARKS = RO_GLOBAL_MARKS + WR_GLOBAL_MARKS

  /** Marks: .^[]"abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ   */
  const val DEL_MARKS = DEL_FILE_MARKS + DEL_GLOBAL_MARKS

  /** Marks: abcdefghijklmnopqrstuvwxyz'.^[]"  */
  const val SAVE_FILE_MARKS = WR_FILE_MARKS + DEL_CONTEXT_FILE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789  */
  const val GLOBAL_MARKS = WR_GLOBAL_MARKS + RO_GLOBAL_MARKS

  /** Marks: abcdefghijklmnopqrstuvwxyz'[]<>^{}()  */
  const val FILE_MARKS = WR_FILE_MARKS + RO_FILE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'  */
  const val WRITE_MARKS = WR_GLOBAL_MARKS + WR_FILE_MARKS

  /** Marks: 0123456789.[]<>^{}()  */
  const val READONLY_MARKS = RO_GLOBAL_MARKS + RO_FILE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'  */
  const val VALID_SET_MARKS = WRITE_MARKS

  /** Marks: ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'0123456789.[]<>^{}()  */
  const val VALID_GET_MARKS = WRITE_MARKS + READONLY_MARKS
}
