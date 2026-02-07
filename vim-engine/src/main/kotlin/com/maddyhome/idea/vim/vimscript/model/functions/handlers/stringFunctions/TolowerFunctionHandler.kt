/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler
import java.util.*

@VimscriptFunction(name = "tolower")
internal class TolowerFunctionHandler : UnaryFunctionHandler<VimString>() {
  override fun doFunction(arguments: Arguments, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) =
    VimString(arguments.getString(0).value.lowercase(Locale.getDefault()))
}
