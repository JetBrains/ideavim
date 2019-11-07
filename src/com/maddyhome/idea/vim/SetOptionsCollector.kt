package com.maddyhome.idea.vim

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SelectModeOptionData

class VimSetOptionsCollector : ApplicationUsagesCollector() {
  override fun getGroupId() = "vim.setcommands"

  override fun getVersion() = 1

  override fun getMetrics() = mutableSetOf<MetricEvent>().apply {
    add(newMetric("enabled.ideaput", OptionsManager.clipboard.contains(ClipboardOptionsData.ideaput)))
    add(newMetric("enabled.ideamarks", OptionsManager.ideamarks.isSet))
    add(newMetric("selected.idearefactormode", OptionsManager.idearefactormode.value))
    add(newMetric("enabled.ideaselection", OptionsManager.selectmode.contains(SelectModeOptionData.ideaselection)))
    add(newMetric("enabled.ideajoin", OptionsManager.ideajoin.isSet))
  }
}
