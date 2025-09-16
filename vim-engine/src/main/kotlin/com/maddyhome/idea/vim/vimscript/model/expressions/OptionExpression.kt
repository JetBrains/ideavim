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
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class OptionExpression(val scope: Scope?, val optionName: String) : LValueExpression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val option = injector.optionGroup.getOption(optionName)
      ?: throw exExceptionMessage("E518", originalString)
    return injector.optionGroup.getOptionValue(option, getAccessScope(editor))
  }

  /**
   * Option expressions are strongly typed, either String or Number.
   *
   * This means that we will not try and use an arithmetic operation on a String option, or a string concatenation
   * operation on a Number option.
   */
  override fun isStronglyTyped() = true

  override fun assign(
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
    val option = injector.optionGroup.getOption(optionName)
      ?: throw exExceptionMessage("E518", originalString)

    // Unlike other Vim variables, options are strongly typed, and we can only assign a Number to a number-based
    // option and a String to a string-based option. Note that we will convert Float to a String, which is not normal
    // Vim rules.
    val newValue = when (option) {
      is NumberOption, is ToggleOption -> if (value is VimString) {
        val number = value.toVimNumber()
        if (number.value == 0 && !value.value.startsWith('0')) {
          // TODO: This should be E521: Number required: &{option}='{value}'
          // Instead, we have E521: Number required after =: '{value}'
          throw exExceptionMessage("E521", "'${value.value}'")
        }
        number
      }
      else value.toVimNumber()
      is StringOption, is StringListOption -> if (value is VimFloat) VimString(value.toOutputString()) else value.toVimString()
      else -> value
    }
    injector.optionGroup.setOptionValue(option, getAccessScope(editor), newValue)
  }

  private fun getAccessScope(editor: VimEditor) = when (scope) {
    Scope.GLOBAL_VARIABLE -> OptionAccessScope.GLOBAL(editor)
    Scope.LOCAL_VARIABLE -> OptionAccessScope.LOCAL(editor)
    null -> OptionAccessScope.EFFECTIVE(editor)
    else -> throw ExException("Invalid option scope")
  }
}
