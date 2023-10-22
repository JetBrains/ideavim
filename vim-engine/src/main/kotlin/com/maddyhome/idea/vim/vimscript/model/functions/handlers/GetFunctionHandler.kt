/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "get")
internal class GetFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments: Int = 2
  override val maximumNumberOfArguments: Int = 3

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val container = argumentValues[0].evaluate(editor, context, vimContext)
    return when (container) {
      is VimList -> {
        val idx = argumentValues[1].evaluate(editor, context, vimContext).asDouble().toInt()
        container.values.getOrElse(idx) {
          argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext) ?: VimInt(-1)
        }
      }
      is VimDictionary -> {
        val key = argumentValues[1].evaluate(editor, context, vimContext).asString()
        container.dictionary.getOrElse(VimString(key)) {
          argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext) ?: VimInt(0)
        }
      }
      is VimBlob, is VimFuncref -> throw ExException("Blobs and Funcref are not supported as an argument for get(). If you need it, request support in YouTrack")
      else -> throw ExException("E896: Argument of get() must be a List, Dictionary or Blob")
    }
  }
}
