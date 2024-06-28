/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.macro

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.MacroRecordingListener
import com.maddyhome.idea.vim.ui.widgets.VimWidgetListener
import com.maddyhome.idea.vim.ui.widgets.mode.VimStatusBarWidget
import java.awt.Component

private const val ID = "IdeaVimMacro"

internal class MacroWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String {
    return ID
  }

  override fun getDisplayName(): String {
    return "IdeaVim Macro Recording Widget"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return VimMacroWidget()
  }

  override fun isAvailable(project: Project): Boolean {
    return VimPlugin.isEnabled() && injector.globalOptions().showmode
  }
}

class VimMacroWidget : StatusBarWidget, VimStatusBarWidget {
  var content: String = ""

  override fun ID(): String {
    return ID
  }

  override fun getPresentation(): StatusBarWidget.WidgetPresentation {
    return VimModeWidgetPresentation()
  }

  private inner class VimModeWidgetPresentation : StatusBarWidget.TextPresentation {
    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getText(): String {
      return content
    }

    override fun getTooltipText(): String {
      return content.ifEmpty {
        "No macro recording in progress"
      }
    }
  }
}

fun updateMacroWidget() {
  val factory = StatusBarWidgetFactory.EP_NAME.findExtension(MacroWidgetFactory::class.java) ?: return
  for (project in ProjectManager.getInstance().openProjects) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateWidget(factory)
  }
}

// TODO: At the moment recording macro & RegisterGroup is bound to application, so macro will be recorded even if we
// move between projects. BUT it's not a good idea. Maybe RegisterGroup should have it's own project scope instances
class MacroWidgetListener : MacroRecordingListener, VimWidgetListener({ updateMacroWidget() }) {
  override fun recordingStarted() {
    for (project in ProjectManager.getInstance().openProjects) {
      val macroWidget = getWidget(project) ?: continue
      val register = injector.registerGroup.recordRegister
      macroWidget.content = "recording @$register"
      macroWidget.updateWidgetInStatusBar(ID, project)
    }
  }

  override fun recordingFinished() {
    for (project in ProjectManager.getInstance().openProjects) {
      val macroWidget = getWidget(project) ?: continue
      macroWidget.content = ""
      macroWidget.updateWidgetInStatusBar(ID, project)
    }
  }

  private fun getWidget(project: Project): VimMacroWidget? {
    val statusBar = WindowManager.getInstance()?.getStatusBar(project) ?: return null
    return statusBar.getWidget(ID) as? VimMacroWidget
  }
}

val macroWidgetOptionListener: VimWidgetListener = VimWidgetListener { updateMacroWidget() }
