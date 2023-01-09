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
import com.intellij.internal.statistic.eventLog.events.BooleanEventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.eventLog.events.StringEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService

internal class OptionsState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val optionService = VimPlugin.getOptionService()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN withOption IjVimOptionService.ideajoin,
        IDEAMARKS withOption IjVimOptionService.ideamarks,
        IDEAREFACTOR withOption IjVimOptionService.idearefactormode,
        IDEAPUT with optionService.contains(OptionScope.GLOBAL, OptionConstants.clipboard, OptionConstants.clipboard_ideaput),
        IDEASTATUSICON withOption IjVimOptionService.ideastatusicon,
        IDEAWRITE withOption IjVimOptionService.ideawrite,
        IDEASELECTION with optionService.contains(OptionScope.GLOBAL, OptionConstants.selectmode, "ideaselection"),
        IDEAVIMSUPPORT with optionService.getValues(OptionScope.GLOBAL, IjVimOptionService.ideavimsupport)!!
      )
    )
  }

  private infix fun BooleanEventField.withOption(name: String): EventPair<Boolean> {
    return this.with(VimPlugin.getOptionService().isSet(OptionScope.GLOBAL, name))
  }

  private infix fun StringEventField.withOption(name: String): EventPair<String?> {
    return this.with(VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, name).asString())
  }

  companion object {
    private val GROUP = EventLogGroup("vim.options", 1)

    private val IDEAJOIN = BooleanEventField(IjVimOptionService.ideajoin)
    private val IDEAMARKS = BooleanEventField(IjVimOptionService.ideamarks)
    private val IDEAREFACTOR = EventFields.String(IjVimOptionService.ideamarks, IjVimOptionService.ideaRefactorModeValues.toList())
    private val IDEAPUT = BooleanEventField("ideaput")
    private val IDEASTATUSICON = EventFields.String(IjVimOptionService.ideastatusicon, IjVimOptionService.ideaStatusIconValues.toList())
    private val IDEAWRITE = EventFields.String(IjVimOptionService.ideawrite, IjVimOptionService.ideaWriteValues.toList())
    private val IDEASELECTION = BooleanEventField("ideaselection")
    private val IDEAVIMSUPPORT = EventFields.StringList(IjVimOptionService.ideavimsupport, IjVimOptionService.ideavimsupportValues.toList())

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
