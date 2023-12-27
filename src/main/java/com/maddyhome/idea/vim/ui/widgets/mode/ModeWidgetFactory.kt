/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener

public class ModeWidgetFactory : StatusBarWidgetFactory {
  public companion object {
    public const val ID: String = "IdeaVim::Mode"
  }

  override fun getId(): String {
    return ID
  }

  override fun getDisplayName(): String {
    return "IdeaVim Mode Widget"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return VimModeWidget(project)
  }

  override fun isAvailable(project: Project): Boolean {
    return VimPlugin.isEnabled() && injector.globalIjOptions().showmodewidget
  }
}

internal object ModeWidgetListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    val factory = StatusBarWidgetFactory.EP_NAME.findExtension(ModeWidgetFactory::class.java) ?: return
    for (project in ProjectManager.getInstance().openProjects) {
      val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
      statusBarWidgetsManager.updateWidget(factory)
    }
  }
}