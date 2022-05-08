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

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.TabInfo
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.newapi.ij

internal class TabServiceImpl : TabService {

  override fun removeTabAt(indexToDelete: Int, indexToSelect: Int, context: ExecutionContext) {
    val tabbedPane = getTabbedPane(context)
    tabbedPane.removeTabAt(indexToDelete, indexToDelete)
  }

  override fun getTabCount(context: ExecutionContext): Int {
    val tabs = getTabs(context)
    return tabs.tabCount
  }

  override fun getCurrentTabIndex(context: ExecutionContext): Int {
    val currentTab = getCurrentTab(context)
    return getTabs(context).getIndexOf(currentTab)
  }

  override fun moveCurrentTabToIndex(index: Int, context: ExecutionContext) {
    val tab = getCurrentTab(context) ?: throw ExException("There is no tab selected")
    val tabs = getTabs(context)
    tabs.removeTab(tab)
    tabs.addTab(tab, index)
    tabs.select(tab, true)
  }

  override fun closeAllExceptCurrentTab(context: ExecutionContext) {
    val currentWindow = getCurrentWindow(context)
    currentWindow.closeAllExcept(currentWindow.selectedFile)
  }

  private fun getCurrentTab(context: ExecutionContext): TabInfo? {
    return getTabs(context).selectedInfo
  }

  private fun getCurrentWindow(context: ExecutionContext): EditorWindow {
    val project = PlatformDataKeys.PROJECT.getData(context.ij) ?: throw ExException("Could not get current tab list")
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    return fileEditorManager.currentWindow
  }

  private fun getTabbedPane(context: ExecutionContext): EditorTabbedContainer {
    val currentWindow = getCurrentWindow(context)
    return currentWindow.tabbedPane
  }

  private fun getTabs(context: ExecutionContext): JBTabs {
    val tabbedPane = getTabbedPane(context)
    return tabbedPane.tabs
  }
}
