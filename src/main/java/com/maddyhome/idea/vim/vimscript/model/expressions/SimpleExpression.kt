package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class SimpleExpression(val data: VimDataType) : Expression() {
  constructor(value: Int) : this(VimInt(value))
  constructor(value: Double) : this(VimFloat(value))
  constructor(value: String) : this(VimString(value))
  constructor(value: MutableList<VimDataType>) : this(VimList(value))
  constructor(value: LinkedHashMap<VimString, VimDataType>) : this(VimDictionary(value))

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    return data
  }
}
