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

@VimscriptFunction(name = "trim")
internal class TrimFunctionHandler : FunctionHandler(minArity = 1, maxArity = 3) {
  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val string = argumentValues[0].evaluate(editor, context, vimContext).toVimString().value

    // Optional mask parameter (characters to trim, default is whitespace)
    val mask = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)?.toVimString()?.value

    // Optional direction parameter: 0 = both sides (default), 1 = start only, 2 = end only
    val direction = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: 0

    val result = if (mask == null || mask.isEmpty()) {
      // Default: trim all characters up to 0x20 (including tab, space, NL, CR) plus non-breaking space 0xa0
      val defaultCharsToTrim = { c: Char -> c <= '\u0020' || c == '\u00a0' }
      when (direction) {
        1 -> string.trimStart(defaultCharsToTrim)
        2 -> string.trimEnd(defaultCharsToTrim)
        else -> string.trim(defaultCharsToTrim)
      }
    } else {
      // Trim specific characters
      val charsToTrim = mask.toCharArray()
      when (direction) {
        1 -> string.trimStart { it in charsToTrim }
        2 -> string.trimEnd { it in charsToTrim }
        else -> string.trim { it in charsToTrim }
      }
    }

    return VimString(result)
  }
}
