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
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.group.IjOptionConstants

internal class OptionsState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val optionGroup = VimPlugin.getOptionGroup()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN withOption IjOptionConstants.ideajoin,
        IDEAMARKS withOption IjOptionConstants.ideamarks,
        IDEAREFACTOR withOption IjOptionConstants.idearefactormode,
        IDEAPUT with optionGroup.contains(OptionScope.GLOBAL, OptionConstants.clipboard, OptionConstants.clipboard_ideaput),
        IDEASTATUSICON withOption IjOptionConstants.ideastatusicon,
        IDEAWRITE withOption IjOptionConstants.ideawrite,
        IDEASELECTION with optionGroup.contains(OptionScope.GLOBAL, OptionConstants.selectmode, "ideaselection"),
        IDEAVIMSUPPORT with optionGroup.getValues(OptionScope.GLOBAL, IjOptionConstants.ideavimsupport)!!
      )
    )
  }

  private infix fun BooleanEventField.withOption(name: String): EventPair<Boolean> {
    return this.with(injector.globalOptions().isSet(name))
  }

  private infix fun StringEventField.withOption(name: String): EventPair<String?> {
    return this.with(injector.globalOptions().getStringValue(name))
  }

  companion object {
    private val GROUP = EventLogGroup("vim.options", 1)

    private val IDEAJOIN = BooleanEventField(IjOptionConstants.ideajoin)
    private val IDEAMARKS = BooleanEventField(IjOptionConstants.ideamarks)
    private val IDEAREFACTOR = EventFields.String(IjOptionConstants.ideamarks, IjOptionConstants.ideaRefactorModeValues.toList())
    private val IDEAPUT = BooleanEventField("ideaput")
    private val IDEASTATUSICON = EventFields.String(IjOptionConstants.ideastatusicon, IjOptionConstants.ideaStatusIconValues.toList())
    private val IDEAWRITE = EventFields.String(IjOptionConstants.ideawrite, IjOptionConstants.ideaWriteValues.toList())
    private val IDEASELECTION = BooleanEventField("ideaselection")
    private val IDEAVIMSUPPORT = EventFields.StringList(IjOptionConstants.ideavimsupport, IjOptionConstants.ideavimsupportValues.toList())

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
