/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
import com.intellij.internal.statistic.eventLog.events.BooleanEventField
import com.intellij.internal.statistic.eventLog.events.EnumEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.services.OptionService

class VimStatistic : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val optionService = VimPlugin.getOptionService()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN.with(optionService.isSet(OptionService.Scope.GLOBAL, "ideajoin")),
        IDEAMARKS.with(optionService.isSet(OptionService.Scope.GLOBAL, "ideamarks")),
        IDEAREFACTOR.with(optionService.getOptionValue(OptionService.Scope.GLOBAL, "idearefactormode").asString().toRefactor())
      )
    )
  }

  companion object {
    private val GROUP = EventLogGroup("vim.options", 1)

    private val IDEAJOIN = BooleanEventField("ideajoin")
    private val IDEAMARKS = BooleanEventField("ideamarks")
    private val IDEAREFACTOR = EnumEventField("ideamarks", IdeaRefactorMode::class.java, IdeaRefactorMode::toString)
//    val IDEAPUT = BooleanEventField("ideaput")

    private val OPTIONS: VarargEventId = GROUP.registerVarargEvent(
      "vim.options",
      IDEAJOIN,
      IDEAMARKS,
      IDEAREFACTOR,
//      IDEAPUT,
    )
  }
}

private enum class IdeaRefactorMode {
  KEEP,
  SELECT,
  VISUAL,
  UNKNOWN;

  override fun toString(): String = when (this) {
    KEEP -> "keep"
    SELECT -> "select"
    VISUAL -> "visual"
    UNKNOWN -> "unknown"
  }

  companion object {
    fun fromString(string: String) = when (string) {
      "keep" -> KEEP
      "select" -> SELECT
      "visual" -> VISUAL
      else -> UNKNOWN
    }
  }
}

private fun String.toRefactor() = IdeaRefactorMode.fromString(this)