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
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ij
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
    return VimPlugin.isEnabled() && injector.globalIjOptions().showmodewidget
  }
}

public class VimMacroWidget : StatusBarWidget, VimStatusBarWidget {
  public var content: String = ""

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

public fun updateMacroWidget() {
  val factory = StatusBarWidgetFactory.EP_NAME.findExtension(MacroWidgetFactory::class.java) ?: return
  for (project in ProjectManager.getInstance().openProjects) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateWidget(factory)
  }
}

public class MacroWidgetListener : MacroRecordingListener, VimWidgetListener({ updateMacroWidget() }) {
  override fun recordingStarted(editor: VimEditor) {
    val macroWidget = getWidget(editor) ?: return
    val register = injector.registerGroup.recordRegister
    macroWidget.content = "recording @$register"
    macroWidget.updateWidgetInStatusBar(ID, editor.ij.project)
  }

  override fun recordingFinished(editor: VimEditor) {
    val macroWidget = getWidget(editor) ?: return
    macroWidget.content = ""
    macroWidget.updateWidgetInStatusBar(ID, editor.ij.project)
  }

  private fun getWidget(editor: VimEditor): VimMacroWidget? {
    val project = (editor as IjVimEditor).editor.project ?: return null
    val statusBar = WindowManager.getInstance()?.getStatusBar(project) ?: return null
    return statusBar.getWidget(ID) as? VimMacroWidget
  }
}

public val macroWidgetOptionListener: VimWidgetListener = VimWidgetListener { updateMacroWidget() }
