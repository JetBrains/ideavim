package com.maddyhome.idea.vim.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.util.SmartList
import com.intellij.util.xmlb.annotations.Attribute
import com.maddyhome.idea.vim.command.MappingMode
import javax.swing.KeyStroke

/**
 * Action holder for IdeaVim actions.
 *
 * [implementation] should be subclass of [EditorActionHandlerBase]
 *
 * [modes] ("mappingModes") defines the action modes. E.g. "NO" - action works in normal and op-pending modes.
 *   Warning: V - Visual and Select mode. X - Visual mode. (like vmap and xmap).
 *   Use "ALL" to enable action for all modes.
 *
 * [keys] comma-separated list of keys for the action. E.g. `gt,gT` - action gets executed on `gt` or `gT`
 * Since xml doesn't allow using raw `<` character, use « and » symbols for mappings with modifiers.
 *   E.g. `«C-U»` - CTRL-U (<C-U> in vim notation)
 * If you want to use exactly `<` character, replace it with `&lt;`. E.g. `i&lt;` - i<
 * If you want to use comma in mapping, use `«COMMA»`
 * Do not place a whitespace around the comma!
 */
class ActionBeanClass : AbstractExtensionPointBean() {
  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("mappingModes")
  var modes: String? = null

  @Attribute("keys")
  var keys: String? = null

  val actionId: String get() = implementation?.let { EditorActionHandlerBase.getActionId(it) } ?: ""

  val action: EditorActionHandlerBase by lazy {
    // FIXME. [VERSION UPDATE] change to instantiateClass for 193+
    @Suppress("DEPRECATION")
    this.instantiate<EditorActionHandlerBase>(
      implementation ?: "", ApplicationManager.getApplication().picoContainer)
  }

  fun getParsedKeys(): Set<List<KeyStroke>>? {
    val myKeys = keys ?: return null
    val escapedKeys = myKeys.splitByComma()
    return EditorActionHandlerBase.parseKeysSet(escapedKeys)
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

  private fun String.splitByComma(): List<String> {
    if (this.isEmpty()) return ArrayList()
    val res = SmartList<String>()
    var start = 0
    var current = 0
    while (current < this.length) {
      if (this[current] == ',') {
        res += this.substring(start, current)
        current++
        start = current
      }
      current++
    }
    res += this.substring(start, current)
    return res
  }
}
