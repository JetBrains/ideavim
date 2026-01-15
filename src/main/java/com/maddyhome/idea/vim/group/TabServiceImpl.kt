/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.TabInfo
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.newapi.ij

internal class TabServiceImpl : TabService {

  override fun removeTabAt(indexToDelete: Int, indexToSelect: Int, context: ExecutionContext) {
    val tabbedPane = getTabbedPane(context)
    tabbedPane?.removeTabAt(indexToDelete, indexToSelect)

    val currentTab = getCurrentTab(context) ?: return
    IdeFocusManager.findInstance().requestFocus(currentTab.component, true)
  }

  override fun getTabCount(context: ExecutionContext): Int {
    val tabs = getTabs(context)
    return tabs?.tabCount ?: 0
  }

  override fun getCurrentTabIndex(context: ExecutionContext): Int {
    val currentTab = getCurrentTab(context) ?: throw ExException("There is no tab selected")
    return getTabs(context)?.getIndexOf(currentTab) ?: throw ExException("There are no tabs open")
  }

  override fun moveCurrentTabToIndex(index: Int, context: ExecutionContext) {
    val tab = getCurrentTab(context) ?: throw ExException("There is no tab selected")
    val tabs = getTabs(context) ?: throw ExException("There are no tabs open")
    tabs.removeTab(tab)
    tabs.addTab(tab, index)
    tabs.select(tab, true)
  }

  override fun closeAllExceptCurrentTab(context: ExecutionContext) {
    val currentWindow = getCurrentWindow(context) ?: throw ExException("There is no tab selected")
    currentWindow.closeAllExcept(currentWindow.selectedFile)
  }

  private fun getCurrentTab(context: ExecutionContext): TabInfo? {
    return getTabs(context)?.selectedInfo
  }

  private fun getCurrentWindow(context: ExecutionContext): EditorWindow? {
    val project = PlatformDataKeys.PROJECT.getData(context.ij) ?: throw ExException("Could not get current tab list")
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    return fileEditorManager.currentWindow
  }

  private fun getTabbedPane(context: ExecutionContext): EditorTabbedContainer? {
    val currentWindow = getCurrentWindow(context)
    return currentWindow?.tabbedPane
  }

  private fun getTabs(context: ExecutionContext): JBTabs? {
    val tabbedPane = getTabbedPane(context)
    return tabbedPane?.tabs
  }
}
