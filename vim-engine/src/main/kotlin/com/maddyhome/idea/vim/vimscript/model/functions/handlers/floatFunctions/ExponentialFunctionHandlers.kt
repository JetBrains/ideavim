/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.intellij.vim.annotations.VimscriptFunction
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10

@VimscriptFunction("exp")
internal class ExpFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = exp(argument)
}

@VimscriptFunction("log")
internal class LogFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = ln(argument)
}

@VimscriptFunction("log10")
internal class Log10FunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = log10(argument)
}
