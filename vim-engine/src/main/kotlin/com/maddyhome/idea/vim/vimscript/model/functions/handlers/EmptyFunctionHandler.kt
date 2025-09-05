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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction(name = "empty")
internal class EmptyFunctionHandler : UnaryFunctionHandler() {
  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, vimContext)
    // TODO: 03.08.2021
    // - |v:false|, |v:none| and |v:null| are empty, |v:true| is not.
    val isEmpty = when (argument) {
      is VimList -> argument.values.isEmpty()
      is VimDictionary -> argument.dictionary.isEmpty()
      is VimString -> argument.value.isEmpty()
      is VimInt -> argument.value == 0
      is VimFloat -> argument.value == 0.0
      is VimFuncref -> false
      is VimBlob -> TODO("Not yet implemented")
      else -> error("Unexpected")
    }
    return isEmpty.asVimInt()
  }
}
