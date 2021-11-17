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
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.eventLog.events.StringEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.OptionServiceImpl

class VimStatistic : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val optionService = VimPlugin.getOptionService()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN withOption "ideajoin",
        IDEAMARKS withOption "ideamarks",
        IDEAREFACTOR withOption "idearefactormode",
        IDEAPUT with optionService.contains(OptionService.Scope.GLOBAL, "clipboard", "ideaput"),
        IDEASTATUSICON withOption "ideastatusicon",
        IDEAWRITE withOption "ideawrite",
        IDEASELECTION with optionService.contains(OptionService.Scope.GLOBAL, "selectmode", "ideaselection"),
        IDEAVIMSUPPORT with optionService.getOptionValue(OptionService.Scope.GLOBAL, "ideavimsupport").asString().split(",")
      )
    )
  }

  private infix fun BooleanEventField.withOption(name: String): EventPair<Boolean> {
    return this.with(VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, name))
  }

  private infix fun StringEventField.withOption(name: String): EventPair<String?> {
    return this.with(VimPlugin.getOptionService().getOptionValue(OptionService.Scope.GLOBAL, name).asString())
  }

  companion object {
    private val GROUP = EventLogGroup("vim.options", 1)

    private val IDEAJOIN = BooleanEventField("ideajoin")
    private val IDEAMARKS = BooleanEventField("ideamarks")
    private val IDEAREFACTOR = EventFields.String("ideamarks", OptionServiceImpl.ideaRefactorModeValues.toList())
    private val IDEAPUT = BooleanEventField("ideaput")
    private val IDEASTATUSICON = EventFields.String("ideastatusicon", OptionServiceImpl.ideaStatusIconValues.toList())
    private val IDEAWRITE = EventFields.String("ideawrite", OptionServiceImpl.ideaWriteValues.toList())
    private val IDEASELECTION = BooleanEventField("ideaselection")
    private val IDEAVIMSUPPORT = EventFields.StringList("ideavimsupport", OptionServiceImpl.ideavimsupportValues.toList())

    private val OPTIONS: VarargEventId = GROUP.registerVarargEvent(
      "vim.options",
      IDEAJOIN,
      IDEAMARKS,
      IDEAREFACTOR,
      IDEAPUT,
      IDEASTATUSICON,
      IDEAWRITE,
      IDEASELECTION,
      IDEAVIMSUPPORT,
    )
  }
}
