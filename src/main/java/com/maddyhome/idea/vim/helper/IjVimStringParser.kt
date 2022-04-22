package com.maddyhome.idea.vim.helper

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.VimStringParser
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

@Service
class IjVimStringParser : VimStringParser {
  override val plugKeyStroke: KeyStroke
    get() = StringHelper.parseKeys("<Plug>")[0]

  override fun parseKeys(vararg strings: String): List<KeyStroke> {
    return StringHelper.parseKeys(*strings)
  }

  override fun stringToKeys(string: String): List<KeyStroke> {
    return StringHelper.stringToKeys(string)
  }

  override fun toKeyNotation(keyStroke: KeyStroke): String {
    return StringHelper.toKeyNotation(keyStroke)
  }

  override fun toKeyNotation(keyStrokes: List<KeyStroke>): String {
    return StringHelper.toKeyNotation(keyStrokes)
  }

  override fun parseKeysSet(vararg keys: String): Set<List<KeyStroke>> {
    return EditorActionHandlerBase.parseKeysSet(*keys)
  }
}
