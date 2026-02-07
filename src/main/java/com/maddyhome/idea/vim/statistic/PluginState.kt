/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.statistic.PluginState.Util.enabledExtensions
import com.maddyhome.idea.vim.statistic.PluginState.Util.extensionNames
import com.maddyhome.idea.vim.ui.JoinEap
import java.util.concurrent.ConcurrentHashMap

internal class PluginState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    return setOf(
      PLUGIN_STATE.metric(
        PLUGIN_ENABLED with VimPlugin.isEnabled(),
        IS_EAP with JoinEap.eapActive(),
        ENABLED_EXTENSIONS with enabledExtensions.toList(),
      ),
    )
  }

  object Util {
    internal val extensionNames = listOf(
      "textobj-entire",
      "argtextobj",
      "ReplaceWithRegister",
      "vim-paragraph-motion",
      "highlightedyank",
      "multiple-cursors",
      "exchange",
      "NERDTree",
      "surround",
      "commentary",
      "matchit",
      "textobj-indent",
      "mini-ai"
    )
    internal val enabledExtensions = ConcurrentHashMap.newKeySet<String>()
  }
}

private val GROUP = EventLogGroup("vim.common", 1, "FUS", "Group: General information about IdeaVim")
private val PLUGIN_ENABLED = EventFields.Boolean("is_plugin_enabled")
private val IS_EAP = EventFields.Boolean("is_EAP_active")
private val ENABLED_EXTENSIONS = EventFields.StringList("enabled_extensions", extensionNames)
private val PLUGIN_STATE: VarargEventId = GROUP.registerVarargEvent(
  "vim.common",
  "State of the IdeaVim plugin",
  PLUGIN_ENABLED,
  IS_EAP,
  ENABLED_EXTENSIONS,
)