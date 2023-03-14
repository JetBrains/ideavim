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
import com.intellij.internal.statistic.eventLog.events.StringListEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption

internal class OptionsState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val globalOptions = injector.globalOptions()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN withOption IjOptionConstants.ideajoin,
        IDEAMARKS withOption IjOptionConstants.ideamarks,
        IDEAREFACTOR withOption IjOptionConstants.idearefactormode,
        IDEAPUT with globalOptions.hasValue(Options.clipboard, OptionConstants.clipboard_ideaput),
        IDEASTATUSICON withOption IjOptionConstants.ideastatusicon,
        IDEAWRITE withOption IjOptionConstants.ideawrite,
        IDEASELECTION with globalOptions.hasValue(Options.selectmode, "ideaselection"),
        IDEAVIMSUPPORT withOption IjOptionConstants.ideavimsupport,
      ),
    )
  }

  private infix fun BooleanEventField.withOption(name: String): EventPair<Boolean> {
    val option = injector.optionGroup.getOption(name) as ToggleOption
    return this.with(injector.globalOptions().isSet(option))
  }

  private infix fun StringEventField.withOption(name: String): EventPair<String?> {
    val option = injector.optionGroup.getOption(name) as StringOption
    return this.with(injector.globalOptions().getStringValue(option))
  }

  private infix fun StringListEventField.withOption(name: String): EventPair<List<String>> {
    val option = injector.optionGroup.getOption(name) as StringOption
    return this.with(injector.globalOptions().getStringListValues(option))
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
