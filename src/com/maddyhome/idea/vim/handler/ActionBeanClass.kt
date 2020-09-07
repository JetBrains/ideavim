/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
 *
 *
 * !! IMPORTANT !!
 * You may wonder why the extension points are used instead of any other approach to register actions.
 *   The reason is startup performance. Using the extension points you don't even have to load classes of actions.
 *   So, all actions are loaded on demand, including classes in classloader.
 */
// [VERSION UPDATE] 202+
@Suppress("DEPRECATION")
class ActionBeanClass : AbstractExtensionPointBean() {
  @Attribute("implementation")
  var implementation: String? = null

  @Attribute("mappingModes")
  var modes: String? = null

  @Attribute("keys")
  var keys: String? = null

  val actionId: String get() = implementation?.let { EditorActionHandlerBase.getActionId(it) } ?: ""

  val action: EditorActionHandlerBase by lazy {
    this.instantiateClass<EditorActionHandlerBase>(
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
