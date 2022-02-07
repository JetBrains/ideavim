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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.TabInfo
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.lang.NumberFormatException

/*
 * see "h :tabmove"
 */
data class TabMoveCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    if (ranges.size() != 0) {
      throw ExException("Range form of tabmove command is not supported. Please use the argument form")
    }

    val project = PlatformDataKeys.PROJECT.getData(context) ?: return ExecutionResult.Error
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val currentWindow = fileEditorManager.currentWindow
    val tabbedPane = currentWindow.tabbedPane
    val tabs = tabbedPane.tabs
    val currentTab = tabs.selectedInfo ?: return ExecutionResult.Error

    val currentIndex = tabs.getIndexOf(currentTab)
    val index: Int

    try {
      index = if (argument.startsWith("+")) {
        val number = argument.substring(1)
        currentIndex + Integer.parseInt(number)
      } else if (argument.startsWith("-")) {
        val number = argument.substring(1)
        currentIndex - Integer.parseInt(number)
      } else if (argument == "$" || argument.isBlank()) {
        tabbedPane.tabCount - 1
      } else {
        var number = Integer.parseInt(argument)

        // it's strange, but it is the way Vim works
        if (number > currentIndex) number -= 1

        number
      }
    } catch (e: NumberFormatException) {
      throw ExException("E474: Invalid argument")
    }

    if (index < 0 || index >= tabbedPane.tabCount) {
      throw ExException("E474: Invalid argument")
    }
    moveTabToIndex(currentTab, index, tabs)

    return ExecutionResult.Success
  }

  private fun moveTabToIndex(tab: TabInfo, index: Int, tabs: JBTabs) {
    tabs.removeTab(tab)
    tabs.addTab(tab, index)
    tabs.select(tab, true)
  }
}