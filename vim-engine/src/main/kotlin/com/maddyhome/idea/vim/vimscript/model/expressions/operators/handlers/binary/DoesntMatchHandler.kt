/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

internal open class DoesntMatchHandlerBase(ignoreCase: Boolean? = null) :
  BinaryOperatorWithIgnoreCaseOption(ignoreCase) {

  override fun performOperation(left: VimDataType, right: VimDataType, ignoreCase: Boolean) =
    injector.regexpService.matches(left.toVimString().value, right.toVimString().value, ignoreCase).asVimInt()
}

internal object DoesntMatchHandler : DoesntMatchHandlerBase()
internal object DoesntMatchIgnoreCaseHandler : DoesntMatchHandlerBase(ignoreCase = true)
internal object DoesntMatchCaseSensitiveHandler : DoesntMatchHandlerBase(ignoreCase = false)
