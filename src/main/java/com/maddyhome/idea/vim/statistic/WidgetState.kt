/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.statistic

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ui.widgets.mode.ModeWidgetFactory

class WidgetState : ApplicationUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  override fun getMetrics(): Set<MetricEvent> {
    return setOf(
      WIDGET.metric(
        IS_MODE_WIDGET_SHOWN with isWidgetShown(),
        MODE_WIDGET_THEME_LIGHT with getModeWidgetTheme("_light"),
        MODE_WIDGET_THEME_DARK with getModeWidgetTheme("_dark"),
      ),
    )
  }

  private fun isWidgetShown(): Boolean {
    for (project in ProjectManager.getInstance().openProjects) {
      val statusBar = WindowManager.getInstance()?.getStatusBar(project) ?: continue
      val widgets = statusBar.allWidgets ?: continue
      if (widgets.any { it.ID() == ModeWidgetFactory.ID }) {
        return true
      }
    }
    return false
  }

  private fun getModeWidgetTheme(postfix: String): String {
    if (injector.variableService.getVimVariable("widget_mode_is_full_customization$postfix")?.asBoolean() == true) {
      return "ADVANCED CUSTOMIZATION"
    }
    val themeString = injector.variableService.getVimVariable("widget_mode_theme$postfix")?.asString()
    return if (themeString?.lowercase() == "colorless") {
      "COLORLESS"
    } else {
      "TERM"
    }
  }

  companion object {
    private val GROUP = EventLogGroup("vim.widget", 1, "FUS")

    private val IS_MODE_WIDGET_SHOWN = EventFields.Boolean("is-mode-widget-shown")
    private val MODE_WIDGET_THEME_LIGHT =
      EventFields.String("mode-widget-theme-light", listOf("TERM", "COLORLESS", "ADVANCED CUSTOMIZATION"))
    private val MODE_WIDGET_THEME_DARK =
      EventFields.String("mode-widget-theme-dark", listOf("TERM", "COLORLESS", "ADVANCED CUSTOMIZATION"))

    private val WIDGET: VarargEventId = GROUP.registerVarargEvent(
      "vim.widget",
      IS_MODE_WIDGET_SHOWN,
      MODE_WIDGET_THEME_LIGHT,
      MODE_WIDGET_THEME_DARK,
    )
  }
}