/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.intellij.vim.annotations.VimscriptFunction

@VimscriptFunction("fmod")
internal class FmodFunctionHandler : BinaryFloatFunctionHandlerBase() {
  override fun invoke(arg1: Double, arg2: Double) = arg1 % arg2
}
