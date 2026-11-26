/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.VimVisualPosition

/**
 * [Error] and [NoMotion] both do not move the caret. However, in case of macros, [Error] stops the macro execution.
 */
sealed class Motion {
  object Error : Motion()
  object NoMotion : Motion()
  open class AbsoluteOffset(val offset: Int) : Motion()

  /**
   * Represents a motion to an absolute offset that has been horizontally adjusted to avoid virtual text.
   *
   * This is most often used during vertical motion, which aims to move the caret to the character column above/below
   * the current location, but also has to avoid virtual text. The intended caret location is remembered as the count of
   * columns from the start of the buffer line (even if wrapped), including the effective column width of all virtual
   * text. This value is used for subsequent vertical motions to correctly re-position the caret in the correct column.
   *
   * @param offset          The absolute offset that the caret will be moved to.
   * @param intendedColumn  The index of the intended location of the caret, as counted in columns from the start of the
   *                        buffer line, including the column width of all virtual text.
   */
  open class AdjustedOffset(offset: Int, val intendedColumn: Int) : AbsoluteOffset(offset)

  /**
   * Represents a motion to a column past the end of the line, allowed by 'virtualedit'.
   *
   * @param lineEndOffset The offset of the end of the line, used by operators and everything that needs a real offset.
   * @param position      The visual position of the caret, with a column that can be past the end of the line.
   * @param intendedColumn The column kept for subsequent vertical motions, see [AdjustedOffset].
   */
  class VirtualColumn(lineEndOffset: Int, val position: VimVisualPosition, intendedColumn: Int = position.column) :
    AdjustedOffset(lineEndOffset, intendedColumn)
}

fun Int.toMotion(): Motion.AbsoluteOffset {
  if (this < 0) error("Unexpected motion: $this")
  return Motion.AbsoluteOffset(this)
}

fun Int.toMotionOrError(): Motion = if (this < 0) Motion.Error else Motion.AbsoluteOffset(this)
fun Long.toMotionOrError(): Motion = if (this < 0) Motion.Error else Motion.AbsoluteOffset(this.toInt())
fun Int.toMotionOrNoMotion(): Motion = if (this < 0) Motion.NoMotion else Motion.AbsoluteOffset(this)
fun Int.toAdjustedMotionOrError(intendedColumn: Int): Motion = if (this < 0) Motion.Error else Motion.AdjustedOffset(this, intendedColumn)
