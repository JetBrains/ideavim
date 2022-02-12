/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.TabInfo
import com.maddyhome.idea.vim.ex.ExException

internal class TabServiceImpl : TabService {

  override fun getTabCount(context: DataContext): Int {
    val tabs = getTabs(context)
    return tabs.tabCount
  }

  override fun getCurrentTabIndex(context: DataContext): Int {
    val currentTab = getCurrentTab(context)
    return getTabs(context).getIndexOf(currentTab)
  }

  override fun moveCurrentTabToIndex(index: Int, context: DataContext) {
    val tab = getCurrentTab(context) ?: throw ExException("There is no tab selected")
    val tabs = getTabs(context)
    tabs.removeTab(tab)
    tabs.addTab(tab, index)
    tabs.select(tab, true)
  }

  private fun getCurrentTab(context: DataContext): TabInfo? {
    return getTabs(context).selectedInfo
  }

  private fun getTabs(context: DataContext): JBTabs {
    val project = PlatformDataKeys.PROJECT.getData(context) ?: throw ExException("Could not get current tab list")
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val currentWindow = fileEditorManager.currentWindow
    val tabbedPane = currentWindow.tabbedPane
    return tabbedPane.tabs
  }
}