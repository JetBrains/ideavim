/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jdom.Element

// Persists popup choices for the mode widget. Readers prefer `g:widget_mode_*` (e.g. set in
// `.ideavimrc`) and fall back to this component, so user config beats remembered popup state.
@State(
  name = "ModeWidgetSettings",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class ModeWidgetSettings : PersistentStateComponent<Element> {
  private val entries: MutableMap<String, String> = mutableMapOf()
  private var migrated = false

  fun getString(key: String): String? {
    ensureMigrated()
    return entries[key]
  }

  fun getBoolean(key: String): Boolean {
    ensureMigrated()
    return entries[key]?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: false
  }

  fun setString(key: String, value: String) {
    entries[key] = value
  }

  fun setBoolean(key: String, value: Boolean) {
    entries[key] = if (value) "1" else "0"
  }

  override fun getState(): Element {
    val element = Element(ROOT_ELEMENT)
    element.setAttribute(MIGRATED_ATTR, migrated.toString())
    for ((key, value) in entries) {
      val entry = Element(ENTRY_ELEMENT)
      entry.setAttribute(KEY_ATTR, key)
      entry.setAttribute(VALUE_ATTR, value)
      element.addContent(entry)
    }
    return element
  }

  override fun loadState(state: Element) {
    migrated = state.getAttributeValue(MIGRATED_ATTR)?.toBoolean() ?: false
    for (entry in state.getChildren(ENTRY_ELEMENT)) {
      val key = entry.getAttributeValue(KEY_ATTR) ?: continue
      val value = entry.getAttributeValue(VALUE_ATTR) ?: continue
      entries[key] = value
    }
  }

  private fun ensureMigrated() {
    if (migrated) return
    migrated = true
    // Older builds stored these values under <vim-variables> in the same XML file; pick them up
    // once so users with existing config don't lose their popup choices on upgrade.
    for (key in LEGACY_KEYS) {
      if (entries.containsKey(key)) continue
      val legacy = injector.variableService.getVimVariable(key) ?: continue
      entries[key] = when (legacy) {
        is VimString -> legacy.value
        is VimInt -> legacy.value.toString()
        else -> continue
      }
    }
  }

  companion object {
    fun getInstance(): ModeWidgetSettings =
      ApplicationManager.getApplication().getService(ModeWidgetSettings::class.java)

    private const val ROOT_ELEMENT = "settings"
    private const val ENTRY_ELEMENT = "entry"
    private const val KEY_ATTR = "key"
    private const val VALUE_ATTR = "value"
    private const val MIGRATED_ATTR = "migrated"

    private val MODE_PREFIXES = listOf(
      "normal", "insert", "replace", "command",
      "visual", "visual_line", "visual_block",
      "select", "select_line", "select_block",
    )

    private val LEGACY_KEYS: List<String> = buildList {
      for (suffix in listOf("_light", "_dark")) {
        add("widget_mode_is_full_customization$suffix")
        add("widget_mode_theme$suffix")
        for (mode in MODE_PREFIXES) {
          add("widget_mode_${mode}_background$suffix")
          add("widget_mode_${mode}_foreground$suffix")
        }
      }
    }
  }
}
