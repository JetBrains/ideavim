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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import kotlin.text.iterator

/**
 * Implementation of Vim's escape() function.
 * Escapes characters specified in the second argument that occur in the first
 * argument with a backslash.
 * Example: escape('c:\program files\vim', ' \') returns 'c:\\program\ files\\vim'
 */
@VimscriptFunction(name = "escape")
internal class EscapeFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments = 2
  override val maximumNumberOfArguments = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    // Get the input string and characters to escape
    val string = argumentValues[0].evaluate(editor, context, vimContext).asString()
    val charsToEscape = argumentValues[1].evaluate(editor, context, vimContext).asString()

    // Process each character in the input string
    val result = StringBuilder()
    for (c in string) {
      // If the current character should be escaped, add a backslash before it
      if (c in charsToEscape) {
        result.append('\\')
      }
      result.append(c)
    }

    return VimString(result.toString())
  }
}
