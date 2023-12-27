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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.MacroRecordingListener
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import java.awt.Component

private const val ID = "IdeaVim::Macro"

internal class MacroWidgetFactory : StatusBarWidgetFactory {
  private var content: String = ""

  private val macroRecordingListener = object : MacroRecordingListener {
    override fun recordingStarted(editor: VimEditor, register: Char) {
      content = "recording @$register"
      updateWidget()
    }

    override fun recordingFinished(editor: VimEditor, register: Char) {
      content = ""
      updateWidget()
    }
  }

  override fun getId(): String {
    return ID
  }

  override fun getDisplayName(): String {
    return "IdeaVim Macro Recording Widget"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    injector.listenersNotifier.macroRecordingListeners.add(macroRecordingListener)
    return VimMacroWidget()
  }

  override fun isAvailable(project: Project): Boolean {
    return VimPlugin.isEnabled() && injector.globalIjOptions().showmodewidget
  }

  private fun updateWidget() {
    val windowManager = WindowManager.getInstance()
    ProjectManager.getInstance().openProjects.forEach {
      val statusBar = windowManager.getStatusBar(it)
      statusBar.updateWidget(ID)
    }
  }

  private inner class VimMacroWidget : StatusBarWidget {
    override fun ID(): String {
      return ID
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation? {
      return VimModeWidgetPresentation()
    }
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

internal object MacroWidgetListener : GlobalOptionChangeListener {
  override fun onGlobalOptionChanged() {
    val factory = StatusBarWidgetFactory.EP_NAME.findExtension(MacroWidgetFactory::class.java) ?: return
    for (project in ProjectManager.getInstance().openProjects) {
      val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
      statusBarWidgetsManager.updateWidget(factory)
    }
  }
}