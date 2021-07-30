package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.option.ListOption
import com.maddyhome.idea.vim.option.NumberOption
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.StringOption
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class OptionExpression(val optionName: String) : Expression() {

  override fun evaluate(editor: Editor?, context: DataContext?, vimContext: VimContext): VimDataType {
    val option = OptionsManager.getOption(optionName) ?: throw ExException("E518: Unknown option: $optionName")
    return when (option) {
      is ListOption -> VimList(option.values().map { VimString(it) }.toMutableList())
      is NumberOption -> VimInt(option.value())
      is StringOption -> VimString(option.value)
      is ToggleOption -> VimInt(if (option.value) 1 else 0)
      else -> throw RuntimeException("Unknown option class passed to option expression: ${option.javaClass}")
    }
  }
}
