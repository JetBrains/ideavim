/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.stringFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "nr2char")
internal class Nr2charFunctionHandler : BuiltinFunctionHandler<VimString>(minArity = 1, maxArity = 2) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    val number = arguments.getNumber(0).value

    // Convert number to character
    // Vim returns empty string for invalid code points
    return try {
      if (number !in 0..0x10FFFF) {
        VimString.EMPTY
      } else {
        VimString(Char(number).toString())
      }
    } catch (_: IllegalArgumentException) {
      VimString.EMPTY
    }
  }
}
