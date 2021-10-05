package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class Register(val char: Char) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    val register = VimPlugin.getRegister().getRegister(char) ?: throw ExException("Register is not supported yet")
    return register.rawText?.let { VimString(it) } ?: VimString("")
  }
}
