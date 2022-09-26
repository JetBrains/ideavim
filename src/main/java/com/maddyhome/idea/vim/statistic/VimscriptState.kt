/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.vimscript.services.VimRcService

internal class VimscriptState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    return setOf(
      VIMSCRIPT.metric(
        SOURCED_FILES with sourcedFiles.size,
        IDEAVIMRC_SIZE with (VimRcService.findIdeaVimRc()?.readLines()?.filter { !it.matches(Regex("\\s*\".*")) && it.isNotBlank() }?.size ?: -1),
        EXTENSIONS_ENABLED_BY_SET with (PluginState.enabledExtensions - extensionsEnabledWithPlug).toList(),
        EXTENSIONS_ENABLED_BY_PLUG with extensionsEnabledWithPlug.toList(),
        IS_IDE_SPECIFIC_CONFIGURATION_USED with isIDESpecificConfigurationUsed,
        IS_LOOP_USED with isLoopUsed,
        IS_IF_USED with isIfUsed,
        IS_MAP_EXPR_USED with isMapExprUsed,
        IS_FUNCTION_DEF_USED with isFunctionDeclarationUsed,
        IS_FUNCTION_CALL_USED with isFunctionCallUsed,
      )
    )
  }

  companion object {
    private val GROUP = EventLogGroup("vim.vimscript", 1)

    val sourcedFiles = HashSet<String>()
    val extensionsEnabledWithPlug = HashSet<String>()
    var isIDESpecificConfigurationUsed = false

    var isLoopUsed = false
    var isIfUsed = false
    var isMapExprUsed = false
    var isFunctionDeclarationUsed = false
    var isFunctionCallUsed = false

    private val SOURCED_FILES = EventFields.RoundedInt("number_of_sourced_files")
    private val IDEAVIMRC_SIZE = EventFields.RoundedInt("ideavimrc_size")
    private val EXTENSIONS_ENABLED_BY_SET = EventFields.StringList("extensions_enabled_by_set", PluginState.extensionNames)
    private val EXTENSIONS_ENABLED_BY_PLUG = EventFields.StringList("extensions_enabled_by_plug", PluginState.extensionNames)
    private val IS_IDE_SPECIFIC_CONFIGURATION_USED = EventFields.Boolean("is_IDE-specific_configuration_used")
    private val IS_LOOP_USED = EventFields.Boolean("is_loop_used")
    private val IS_IF_USED = EventFields.Boolean("is_if_used")
    private val IS_MAP_EXPR_USED = EventFields.Boolean("is_map_expr_used")
    private val IS_FUNCTION_DEF_USED = EventFields.Boolean("is_function_declaration_used")
    private val IS_FUNCTION_CALL_USED = EventFields.Boolean("is_function_call_used")

    private val VIMSCRIPT: VarargEventId = GROUP.registerVarargEvent(
      "vim.vimscript",
      SOURCED_FILES,
      IDEAVIMRC_SIZE,
      EXTENSIONS_ENABLED_BY_SET,
      EXTENSIONS_ENABLED_BY_PLUG,
      IS_IDE_SPECIFIC_CONFIGURATION_USED,
      IS_LOOP_USED,
      IS_IF_USED,
      IS_MAP_EXPR_USED,
      IS_FUNCTION_DEF_USED,
      IS_FUNCTION_CALL_USED,
    )
  }
}
