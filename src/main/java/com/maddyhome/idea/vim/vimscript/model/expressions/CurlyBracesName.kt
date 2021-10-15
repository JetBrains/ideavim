package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class CurlyBracesName(val parts: List<Expression>) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimString {
    return VimString(parts.joinToString(separator = "") { it.evaluate(editor, context, parent).asString() })
  }
}
