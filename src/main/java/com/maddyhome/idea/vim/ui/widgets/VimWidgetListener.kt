/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets

import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.VimPluginListener
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener

public class VimWidgetListener(private val widgetFactory: Class<out StatusBarWidgetFactory>) : GlobalOptionChangeListener, VimPluginListener {
  init {
    injector.listenersNotifier.vimPluginListeners.add(this)
  }

  override fun onGlobalOptionChanged() {
    updateWidget()
  }

  override fun turnedOn() {
    updateWidget()
  }

  override fun turnedOff() {
    updateWidget()
  }

  private fun updateWidget() {
    val factory = StatusBarWidgetFactory.EP_NAME.findExtension(widgetFactory) ?: return
    for (project in ProjectManager.getInstance().openProjects) {
      val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
      statusBarWidgetsManager.updateWidget(factory)
    }
  }
}