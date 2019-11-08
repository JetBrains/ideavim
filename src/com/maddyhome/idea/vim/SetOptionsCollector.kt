package com.maddyhome.idea.vim

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.IdeaRefactorMode
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SelectModeOptionData

class VimSetOptionsCollector : ApplicationUsagesCollector() {
  override fun getGroupId() = "vim.setcommands"

  // TODO: 07.11.2019 [VERSION UPDATE] 192+ should be removed
  @Suppress("UnstableApiUsage", "DEPRECATION")
  override fun getUsages(): MutableSet<UsageDescriptor> = mutableSetOf<UsageDescriptor>().apply {
    add(booleanDescriptor("ideaput", OptionsManager.clipboard.contains(ClipboardOptionsData.ideaput)))
    add(booleanDescriptor("ideamarks", OptionsManager.ideamarks.isSet))
    add(UsageDescriptor("idearefactormode.selected", refactorMode(OptionsManager.idearefactormode.value)))
    add(booleanDescriptor("ideaselection", OptionsManager.selectmode.contains(SelectModeOptionData.ideaselection)))
    add(booleanDescriptor("ideajoin", OptionsManager.ideajoin.isSet))
  }

  @Suppress("UnstableApiUsage", "DEPRECATION")
  private fun booleanDescriptor(key: String, value: Boolean): UsageDescriptor {
    return UsageDescriptor(key + if (value) ".enabled" else ".disabled", 1)
  }

  private fun refactorMode(refactorMode: String): Int = when (refactorMode) {
    IdeaRefactorMode.keep -> 1
    IdeaRefactorMode.select -> 2
    IdeaRefactorMode.visual -> 3
    else -> -1
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
