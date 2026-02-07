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

data class DictionaryExpression(val dictionary: LinkedHashMap<Expression, Expression>) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val dict = VimDictionary(linkedMapOf())
    for ((k, v) in dictionary) {
      val key = k.evaluate(editor, context, vimContext).toVimString()
      val value = v.evaluate(editor, context, vimContext)
      dict.dictionary[key] = value
    }
    return dict
  }
}
