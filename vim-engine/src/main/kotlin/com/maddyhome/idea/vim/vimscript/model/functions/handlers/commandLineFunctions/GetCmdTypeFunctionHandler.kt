/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.commandLineFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

/*
Return the current command-line type. Possible return values are:
    :	normal Ex command
    /	forward search command
    ?	backward search command
    =	i_CTRL-R_=

Returns an empty string otherwise.

Not yet implemented:
    >	debug mode command debug-mode
    @	input() command
    -	:insert or :append command
 */
@VimscriptFunction(name = "getcmdtype")
internal class GetCmdTypeFunctionHandler : FunctionHandlerBase<VimString>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    val mode = editor.mode
    return when (mode) {
      is Mode.CMD_LINE -> VimString(injector.commandLine.getActiveCommandLine()?.getLabel() ?: "")
      else -> VimString.EMPTY
    }
  }
}
