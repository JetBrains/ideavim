/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

sealed class Motion {
  object Error : Motion()
  object NoMotion : Motion()
  class AbsoluteOffset(val offset: Int) : Motion()
}

fun Int.toMotion(): Motion.AbsoluteOffset {
  if (this < 0) error("Unexpected motion: $this")
  return Motion.AbsoluteOffset(this)
}

fun Int.toMotionOrError(): Motion = if (this < 0) Motion.Error else Motion.AbsoluteOffset(this)
fun Long.toMotionOrError(): Motion = if (this < 0) Motion.Error else Motion.AbsoluteOffset(this.toInt())
fun Int.toMotionOrNoMotion(): Motion = if (this < 0) Motion.NoMotion else Motion.AbsoluteOffset(this)
