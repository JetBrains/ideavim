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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class OptionExpression(val scope: Scope?, val optionName: String) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val option = injector.optionGroup.getOption(optionName) ?: throw exExceptionMessage("E518", originalString)
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> injector.optionGroup.getOptionValue(option, OptionAccessScope.GLOBAL(editor))
      Scope.LOCAL_VARIABLE -> injector.optionGroup.getOptionValue(option, OptionAccessScope.LOCAL(editor))
      null -> injector.optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(editor))
      else -> throw ExException("Invalid option scope")
    }
  }
}
