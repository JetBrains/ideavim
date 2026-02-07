/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.NamedFunctionCallExpression

fun VimDataType.toVimFuncref(
  editor: VimEditor,
  context: ExecutionContext,
  scriptContext: VimLContext,
): VimFuncref {
  // We're already a Funcref, e.g., as the result of a `function()` or `funcref()` expression, or a lambda expression,
  // or a dict.item variable lookup
  if (this is VimFuncref) return this

  // Try to convert to a String, which is either a function name, or an expression that resolves to a Funcref (e.g. a
  // variable containing a Funcref, or a `function()` or `funcref()` expression).
  // If we've passed a List or Dictionary, this will throw an error. Vim normally doesn't support converting a Float to
  // a String, but it does in this case
  val string = if (this is VimFloat) VimString(this.toOutputString()) else this.toVimString()

  // If the String is the name of a function, wrap it in a Funcref
  val handler = injector.functionService.getFunctionHandlerOrNull(null, string.value, scriptContext)
  if (handler != null) {
    return VimFuncref(handler, VimList(mutableListOf()), dictionary = null, VimFuncref.Type.FUNCREF)
  }

  // The String should now be either a variable expression or a `function()` or `funcref()` expression, all of which
  // will evaluate to a Funcref.
  val expression = injector.vimscriptParser.parseExpression(string.value)

  val name = try {
    return expression?.evaluate(editor, context, scriptContext) as VimFuncref
  } catch (_: Exception) {
    // Get the argument for function(...) or funcref(...) for the error message
    if (expression is NamedFunctionCallExpression && expression.arguments.isNotEmpty()) {
      expression.arguments[0].evaluate(editor, context, scriptContext).toOutputString()
    } else {
      string.value
    }
  }
  throw exExceptionMessage("E117", name)
}
