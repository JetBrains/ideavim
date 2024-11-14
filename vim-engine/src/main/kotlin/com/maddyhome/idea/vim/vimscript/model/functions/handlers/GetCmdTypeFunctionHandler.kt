/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

/*
Return the current command-line type. Possible return values are:
    :	normal Ex command
    >	debug mode command debug-mode
    /	forward search command
    ?	backward search command
    =	i_CTRL-R_=

Returns an empty string otherwise.

Not yet implemented:
    @	input() command
    -	:insert or :append command
 */
@VimscriptFunction(name = "getcmdtype")
internal class GetCmdTypeFunctionHandler : FunctionHandler() {
    override val minimumNumberOfArguments = 0
    override val maximumNumberOfArguments = 0

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val mode = editor.mode
    return when (mode) {
      is Mode.CMD_LINE -> VimString(injector.commandLine.getActiveCommandLine()?.label ?: "")
      else -> VimString("")
    }
  }

}
