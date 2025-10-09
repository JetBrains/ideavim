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
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.initInjector
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.OptionConstants

internal class OptionsState : ApplicationUsagesCollector() {

  init {
    initInjector()
  }

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    val globalOptions = injector.globalOptions()
    val globalIjOptions = injector.globalIjOptions()

    return setOf(
      // ideajoin and idearefactor area global-local. We're only interested in the global value, not the effective
      // value, which a) might be set at local scope and b) isn't accessible without an editor
      // We don't need to pass a window, since the global values will only ever be global
      OPTIONS.metric(
        IDEAJOIN with injector.optionGroup.getOptionValue(IjOptions.ideajoin, OptionAccessScope.GLOBAL(null)).booleanValue,
        IDEAMARKS with globalIjOptions.ideamarks,
        IDEAREFACTOR with injector.optionGroup.getOptionValue(IjOptions.idearefactormode, OptionAccessScope.GLOBAL(null)).value,
        IDEAPUT with globalOptions.clipboard.contains(OptionConstants.clipboard_ideaput),
        IDEASTATUSICON with globalIjOptions.ideastatusicon,
        IDEAWRITE with globalIjOptions.ideawrite,
        IDEASELECTION with globalOptions.selectmode.contains(OptionConstants.selectmode_ideaselection),
        IDEAVIMSUPPORT with globalIjOptions.ideavimsupport,
      ),
    )
  }

}

private val GROUP = EventLogGroup("vim.options", 1)
private val IDEAJOIN = BooleanEventField(IjOptions.ideajoin.name)
private val IDEAMARKS = BooleanEventField(IjOptions.ideamarks.name)
private val IDEAREFACTOR =
  EventFields.String(IjOptions.idearefactormode.name, IjOptionConstants.ideaRefactorModeValues.toList())
private val IDEAPUT = BooleanEventField(OptionConstants.clipboard_ideaput)
private val IDEASTATUSICON =
  EventFields.String(IjOptions.ideastatusicon.name, IjOptionConstants.ideaStatusIconValues.toList())
private val IDEAWRITE = EventFields.String(IjOptions.ideawrite.name, IjOptionConstants.ideaWriteValues.toList())
private val IDEASELECTION = BooleanEventField(OptionConstants.selectmode_ideaselection)
private val IDEAVIMSUPPORT =
  EventFields.StringList(IjOptions.ideavimsupport.name, IjOptionConstants.ideavimsupportValues.toList())
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
