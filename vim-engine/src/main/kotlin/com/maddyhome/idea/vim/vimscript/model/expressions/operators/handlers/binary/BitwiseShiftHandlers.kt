/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

internal abstract class BitwiseShiftHandler : BinaryOperatorHandler() {
  final override fun performOperation(
    left: VimDataType,
    right: VimDataType,
  ): VimDataType {
    val value = (left as? VimInt)?.value ?: throw exExceptionMessage("E1282")
    val bitCount = (right as? VimInt)?.value ?: throw exExceptionMessage("E1282")
    if (bitCount < 0) throw exExceptionMessage("E1283")
    return if (bitCount >= Int.SIZE_BITS) VimInt.ZERO else VimInt(doShift(value, bitCount))
  }

  abstract fun doShift(value: Int, bitCount: Int): Int
}

internal object BitwiseLeftShiftHandler : BitwiseShiftHandler() {
  override fun doShift(value: Int, bitCount: Int) = value shl bitCount
}

internal object BitwiseRightShiftHandler : BitwiseShiftHandler() {
  override fun doShift(value: Int, bitCount: Int) = value shr bitCount
}
