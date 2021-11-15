package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.BooleanEventField
import com.intellij.internal.statistic.eventLog.events.EventId1
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.option.OptionsManager

class VimStatistic : ApplicationUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    return setOf(OPTIONS.metric(OptionsManager.ideajoin.isSet))
  }

  companion object {
    private val GROUP = EventLogGroup("vim.options", 1)
    private val OPTIONS: EventId1<Boolean> = GROUP.registerEvent("ideajoin", BooleanEventField("ideajoin"))
  }
}
