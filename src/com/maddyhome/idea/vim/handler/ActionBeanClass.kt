package com.maddyhome.idea.vim.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.command.MappingMode
import javax.swing.KeyStroke

class ActionBeanClass : AbstractExtensionPointBean() {
  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("mappingModes")
  var modes: String? = null

  @Attribute("keys")
  var keys: String? = null

  val action: EditorActionHandlerBase by lazy {
    this.instantiateClass<EditorActionHandlerBase>(implementation
      ?: "", ApplicationManager.getApplication().picoContainer)
  }

  fun getParsedKeys(): Set<List<KeyStroke>>? {
    val myKeys = keys ?: return null
    val escapedKeys = myKeys
      .replace('»', '>')
      .replace('«', '<')
      .split(",")
      .dropLastWhile { it.isEmpty() }
      .map { it.replace("<COMMA>", ",") }
      .toTypedArray()
    return EditorActionHandlerBase.parseKeysSet(*escapedKeys)
  }

  fun getParsedModes(): Set<MappingMode>? {
    val myModes = modes ?: return null

    if ("ALL" == myModes) return MappingMode.ALL

    val res = mutableListOf<MappingMode>()
    for (c in myModes) {
      when (c) {
        'N' -> res += MappingMode.NORMAL
        'X' -> res += MappingMode.VISUAL
        'V' -> {
          res += MappingMode.VISUAL
          res += MappingMode.SELECT
        }
        'S' -> res += MappingMode.SELECT
        'O' -> res += MappingMode.OP_PENDING
        'I' -> res += MappingMode.INSERT
        'C' -> res += MappingMode.CMD_LINE
        else -> throw RuntimeException("Wrong mapping mode: $c")
      }
    }
    return res.toSet()
  }
}
