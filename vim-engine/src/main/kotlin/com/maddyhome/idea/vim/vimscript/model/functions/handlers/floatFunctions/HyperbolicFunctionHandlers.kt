/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.intellij.vim.annotations.VimscriptFunction
import kotlin.math.cosh
import kotlin.math.sinh
import kotlin.math.tanh

@VimscriptFunction(name = "cosh")
internal class CoshFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = cosh(argument)
}

@VimscriptFunction(name = "sinh")
internal class SinhFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = sinh(argument)
}

@VimscriptFunction(name = "tanh")
internal class TanhFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = tanh(argument)
}
