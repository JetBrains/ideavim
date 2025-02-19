/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.intellij.vim.annotations.VimscriptFunction
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.truncate

@VimscriptFunction("ceil")
internal class CeilFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = ceil(argument)
}

@VimscriptFunction("floor")
internal class FloorFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = floor(argument)
}

@VimscriptFunction("round")
internal class RoundFunctionHandler : UnaryFloatFunctionHandlerBase() {
  // kotlin.math.round does bankers' rounding
  override fun invoke(argument: Double) = if (argument >= 0) {
    floor(argument + 0.5)
  } else {
    -floor(-argument + 0.5)
  }
}

@VimscriptFunction("trunc")
internal class TruncFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = truncate(argument)
}
