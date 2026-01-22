/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.collectionFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "get")
internal class GetFunctionHandler : BuiltinFunctionHandler<VimDataType>(minArity = 2, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val container = arguments[0]
    return when (container) {
      is VimList -> {
        val idx = arguments.getNumber(1).value
        container.values.getOrElse(idx) { arguments.getOrNull(2) ?: VimInt.ZERO }
      }

      is VimDictionary -> {
        val key = arguments.getString(1)
        container.dictionary.getOrElse(key) { arguments.getOrNull(2) ?: VimInt.ZERO }
      }

      is VimBlob, is VimFuncref -> throw ExException("Blobs and Funcref are not supported as an argument for get(). If you need it, request support in YouTrack")
      else -> throw exExceptionMessage("E896", "get()")
    }
  }
}
