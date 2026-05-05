/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.mappingFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.hasMapTo
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

@VimscriptFunction("hasmapto")
internal class HasMapToFunctionHandler : MapFunctionHandlerBase<VimInt>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val what = arguments.getString(0)
    val mode = arguments.getStringOrNull(1)
    val abbr = arguments.getNumberOrNull(2)?.booleanValue ?: false

    // TODO: IdeaVim should support abbreviations
    if (abbr) {
      return VimInt.ZERO
    }

    val modes = getMappingModes(mode)
    return injector.keyGroup.hasMapTo(what.value, modes).asVimInt()
  }
}
