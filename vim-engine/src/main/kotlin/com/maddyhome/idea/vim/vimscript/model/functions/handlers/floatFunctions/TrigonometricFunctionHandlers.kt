/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.intellij.vim.annotations.VimscriptFunction
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

@VimscriptFunction(name = "acos")
internal class AcosFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = acos(argument)
}

@VimscriptFunction(name = "asin")
internal class AsinFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = asin(argument)
}

@VimscriptFunction(name = "atan")
internal class AtanFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = atan(argument)
}

@VimscriptFunction(name = "atan2")
internal class Atan2FunctionHandler : BinaryFloatFunctionHandlerBase() {
  override fun invoke(arg1: Double, arg2: Double) = atan2(arg1, arg2)
}

@VimscriptFunction(name = "cos")
internal class CosFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = cos(argument)
}

@VimscriptFunction(name = "sin")
internal class SinFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = sin(argument)
}

@VimscriptFunction(name = "tan")
internal class TanFunctionHandler : UnaryFloatFunctionHandlerBase() {
  override fun invoke(argument: Double) = tan(argument)
}
