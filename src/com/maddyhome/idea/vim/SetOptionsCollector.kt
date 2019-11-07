package com.maddyhome.idea.vim

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SelectModeOptionData

class VimSetOptionsCollector : ApplicationUsagesCollector() {
  override fun getGroupId() = "vim.setcommands"

  override fun getVersion() = 1

  // TODO: 07.11.2019 [VERSION UPDATE] 192+ should be removed
  @Suppress("UnstableApiUsage", "DEPRECATION")
  override fun getUsages(): MutableSet<UsageDescriptor> = mutableSetOf<UsageDescriptor>().apply {
    add(booleanDescriptor("enabled.ideaput", OptionsManager.clipboard.contains(ClipboardOptionsData.ideaput)))
    add(booleanDescriptor("enabled.ideamarks", OptionsManager.ideamarks.isSet))
    add(UsageDescriptor("selected.idearefactormode", FeatureUsageData().addData("value", OptionsManager.idearefactormode.value)))
    add(booleanDescriptor("enabled.ideaselection", OptionsManager.selectmode.contains(SelectModeOptionData.ideaselection)))
    add(booleanDescriptor("enabled.ideajoin", OptionsManager.ideajoin.isSet))
  }

  @Suppress("UnstableApiUsage")
  private fun booleanDescriptor(key: String, value: Boolean): UsageDescriptor {
    @Suppress("UnstableApiUsage")
    return UsageDescriptor(key, FeatureUsageData().addData("enabled", value))
  }
}

/*
  Updated version for 192+


  override fun getMetrics() = mutableSetOf<MetricEvent>().apply {
    add(newMetric("enabled.ideaput", OptionsManager.clipboard.contains(ClipboardOptionsData.ideaput)))
    add(newMetric("enabled.ideamarks", OptionsManager.ideamarks.isSet))
    add(newMetric("selected.idearefactormode", OptionsManager.idearefactormode.value))
    add(newMetric("enabled.ideaselection", OptionsManager.selectmode.contains(SelectModeOptionData.ideaselection)))
    add(newMetric("enabled.ideajoin", OptionsManager.ideajoin.isSet))
  }
 */
