/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.search

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.SearchListener
import com.maddyhome.idea.vim.helper.vimLastHighlighters
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.ui.widgets.VimWidgetListener
import com.maddyhome.idea.vim.ui.widgets.mode.VimStatusBarWidget
import java.awt.Component

private const val ID = "IdeaVimSearch"

internal class SearchWidgetFactory : StatusBarWidgetFactory {

  override fun getId(): String {
    return ID
  }

  override fun getDisplayName(): String {
    return "IdeaVim Search"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return VimSearchWidget()
  }

  override fun isAvailable(project: Project): Boolean {
    return VimPlugin.isEnabled() && injector.globalOptions().showmode
  }
}

fun updateSearchWidget() {
  val factory = StatusBarWidgetFactory.EP_NAME.findExtension(SearchWidgetFactory::class.java) ?: return
  for (project in ProjectManager.getInstance().openProjects) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateWidget(factory)
  }
}

class VimSearchWidget : StatusBarWidget, VimStatusBarWidget {
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
        "No search in progress"
      }
    }
  }
}

class SearchWidgetListener : SearchListener, VimWidgetListener({ updateSearchWidget() }) {

  override fun searchUpdated(editor: VimEditor, offset: Int) {
    val ijEditor = editor.ij
    var currentHighlighter = 1
    val numHighlighters = ijEditor.vimLastHighlighters?.size ?: 0
    for (highlighter in ijEditor.vimLastHighlighters!!) {
      if (highlighter.startOffset == offset) {
        break
      }
      currentHighlighter++
    }
    for (project in ProjectManager.getInstance().openProjects) {
      val searchWidget = getWidget(project) ?: continue
      searchWidget.content = String.format("Occurrence: %s of %s", currentHighlighter, numHighlighters)
      searchWidget.updateWidgetInStatusBar(ID, project)
    }
  }

  override fun searchStopped() {
    for (project in ProjectManager.getInstance().openProjects) {
      val searchWidget = getWidget(project) ?: continue
      searchWidget.content = ""
      searchWidget.updateWidgetInStatusBar(ID, project)
    }
  }

  private fun getWidget(project: Project): VimSearchWidget? {
    val statusBar = WindowManager.getInstance()?.getStatusBar(project) ?: return null
    return statusBar.getWidget(ID) as? VimSearchWidget
  }

}

val searchWidgetOptionListener: VimWidgetListener = VimWidgetListener { updateSearchWidget() }
