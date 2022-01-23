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
import com.intellij.internal.statistic.eventLog.events.BooleanEventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.eventLog.events.StringEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.services.OptionConstants
import com.maddyhome.idea.vim.vimscript.services.OptionService
import com.maddyhome.idea.vim.vimscript.services.OptionServiceImpl

class VimStatistic : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val optionService = VimPlugin.getOptionService()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN withOption OptionConstants.ideajoinName,
        IDEAMARKS withOption OptionConstants.ideamarksName,
        IDEAREFACTOR withOption OptionConstants.idearefactormodeName,
        IDEAPUT with optionService.contains(OptionService.Scope.GLOBAL, OptionConstants.clipboardName, "ideaput"),
        IDEASTATUSICON withOption OptionConstants.ideastatusiconName,
        IDEAWRITE withOption OptionConstants.ideawriteName,
        IDEASELECTION with optionService.contains(OptionService.Scope.GLOBAL, OptionConstants.selectmodeName, "ideaselection"),
        IDEAVIMSUPPORT with optionService.getValues(OptionService.Scope.GLOBAL, OptionConstants.ideavimsupportName)!!
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

    private val IDEAJOIN = BooleanEventField(OptionConstants.ideajoinName)
    private val IDEAMARKS = BooleanEventField(OptionConstants.ideamarksName)
    private val IDEAREFACTOR = EventFields.String(OptionConstants.ideamarksName, OptionServiceImpl.ideaRefactorModeValues.toList())
    private val IDEAPUT = BooleanEventField("ideaput")
    private val IDEASTATUSICON = EventFields.String(OptionConstants.ideastatusiconName, OptionServiceImpl.ideaStatusIconValues.toList())
    private val IDEAWRITE = EventFields.String(OptionConstants.ideawriteName, OptionServiceImpl.ideaWriteValues.toList())
    private val IDEASELECTION = BooleanEventField("ideaselection")
    private val IDEAVIMSUPPORT = EventFields.StringList(OptionConstants.ideavimsupportName, OptionServiceImpl.ideavimsupportValues.toList())

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
