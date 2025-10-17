/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler

data class DictionaryExpression(val dictionary: LinkedHashMap<Expression, Expression>) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val dict = VimDictionary(linkedMapOf())
    for ((key, value) in dictionary) {
      val evaluatedVal = value.evaluate(editor, context, vimContext)
      var newFuncref = evaluatedVal
      if (evaluatedVal is VimFuncref && evaluatedVal.handler is DefinedFunctionHandler && !evaluatedVal.isSelfFixed) {
        newFuncref = evaluatedVal.deepCopy(0)
        newFuncref.dictionary = dict
      }
      dict.dictionary[key.evaluate(editor, context, vimContext).toVimString()] = newFuncref
    }
    return dict
  }
}
