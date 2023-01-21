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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class OptionExpression(val scope: Scope, val optionName: String) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> injector.optionGroup.getOptionValue(OptionScope.GLOBAL, optionName, originalString)
      Scope.LOCAL_VARIABLE -> injector.optionGroup.getOptionValue(OptionScope.LOCAL(editor), optionName, originalString)
      else -> throw ExException("Invalid option scope")
    }
  }
}
