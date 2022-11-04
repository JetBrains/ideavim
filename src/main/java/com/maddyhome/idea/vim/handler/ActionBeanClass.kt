/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.serviceContainer.BaseKeyedLazyInstance
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
 *
 *
 * !! IMPORTANT !!
 * You may wonder why the extension points are used instead of any other approach to register actions.
 *   The reason is startup performance. Using the extension points you don't even have to load classes of actions.
 *   So, all actions are loaded on demand, including classes in classloader.
 */
class ActionBeanClass : BaseKeyedLazyInstance<EditorActionHandlerBase>() {
  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("mappingModes")
  var modes: String? = null

  @Attribute("keys")
  var keys: String? = null

  val actionId: String get() = implementation?.let { EditorActionHandlerBase.getActionId(it) } ?: ""

  fun getParsedKeys(): Set<List<KeyStroke>>? {
    val myKeys = keys ?: return null
    val escapedKeys = myKeys.splitByComma()
    return EditorActionHandlerBase.parseKeysSet(escapedKeys)
  }

  override fun getImplementationClassName(): String? = implementation

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
        else -> error("Wrong mapping mode: $c")
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
