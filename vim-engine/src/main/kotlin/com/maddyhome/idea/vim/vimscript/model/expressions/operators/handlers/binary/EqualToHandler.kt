/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

internal class EqualToHandler(ignoreCase: Boolean? = null) : EqualToHandlerBase(ignoreCase)

internal class NotEqualToHandler(ignoreCase: Boolean? = null) : EqualToHandlerBase(ignoreCase) {
  override fun performOperation(left: VimDataType, right: VimDataType, ignoreCase: Boolean): VimDataType {
    val result = super.performOperation(left, right, ignoreCase) as VimInt
    return (!result.booleanValue).asVimInt()
  }
}

internal open class EqualToHandlerBase(ignoreCase: Boolean? = null) : ComparisonOperatorHandler(ignoreCase) {
  override fun compare(left: Double, right: Double) = left == right
  override fun compare(left: Int, right: Int) = left == right
  override fun compare(left: String, right: String, ignoreCase: Boolean) = left.compareTo(right, ignoreCase) == 0

  override fun compare(left: VimList, right: VimList, ignoreCase: Boolean) =
    left.valueEquals(right, ignoreCase, depth = 0)

  override fun compare(left: VimDictionary, right: VimDictionary, ignoreCase: Boolean) =
    left.valueEquals(right, ignoreCase, depth = 0)

  override fun compare(left: VimFuncref, right: VimFuncref, ignoreCase: Boolean) =
    left.valueEquals(right, ignoreCase, depth = 0)

  // TODO: Implement for Blob
}
