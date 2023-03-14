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
import com.intellij.internal.statistic.eventLog.events.StringEventField
import com.intellij.internal.statistic.eventLog.events.StringListEventField
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption

internal class OptionsState : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val globalOptions = injector.globalOptions()

    return setOf(
      OPTIONS.metric(
        IDEAJOIN withOption IjOptions.ideajoin,
        IDEAMARKS withOption IjOptions.ideamarks,
        IDEAREFACTOR withOption IjOptions.idearefactormode,
        IDEAPUT with globalOptions.hasValue(Options.clipboard, OptionConstants.clipboard_ideaput),
        IDEASTATUSICON withOption IjOptions.ideastatusicon,
        IDEAWRITE withOption IjOptions.ideawrite,
        IDEASELECTION with globalOptions.hasValue(Options.selectmode, "ideaselection"),
        IDEAVIMSUPPORT withOption IjOptions.ideavimsupport,
      ),
    )
  }

  private infix fun BooleanEventField.withOption(option: ToggleOption) =
    this.with(injector.globalOptions().isSet(option))

  private infix fun StringEventField.withOption(option: StringOption) =
    this.with(injector.globalOptions().getStringValue(option))

  private infix fun StringListEventField.withOption(option: StringOption) =
    this.with(injector.globalOptions().getStringListValues(option))

  companion object {
    private val GROUP = EventLogGroup("vim.options", 1)

    private val IDEAJOIN = BooleanEventField(IjOptions.ideajoin.name)
    private val IDEAMARKS = BooleanEventField(IjOptions.ideamarks.name)
    // TODO: This looks like the wrong name!!
    private val IDEAREFACTOR = EventFields.String(IjOptions.ideamarks.name, IjOptionConstants.ideaRefactorModeValues.toList())
    private val IDEAPUT = BooleanEventField("ideaput")
    private val IDEASTATUSICON = EventFields.String(IjOptions.ideastatusicon.name, IjOptionConstants.ideaStatusIconValues.toList())
    private val IDEAWRITE = EventFields.String(IjOptions.ideawrite.name, IjOptionConstants.ideaWriteValues.toList())
    private val IDEASELECTION = BooleanEventField("ideaselection")
    private val IDEAVIMSUPPORT = EventFields.StringList(IjOptions.ideavimsupport.name, IjOptionConstants.ideavimsupportValues.toList())

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
