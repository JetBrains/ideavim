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

internal object MatchesHandler : BinaryOperatorWithIgnoreCaseOption(MatchesIgnoreCaseHandler, MatchesCaseSensitiveHandler)

internal object MatchesIgnoreCaseHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType) =
    injector.regexpService.matches(right.toVimString().value, left.toVimString().value, ignoreCase = true).asVimInt()
}

internal object MatchesCaseSensitiveHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType) =
    injector.regexpService.matches(right.toVimString().value, left.toVimString().value, ignoreCase = false).asVimInt()
}
