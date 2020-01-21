/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.action.fold

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler

class VimCollapseAllRegions : VimActionHandler.SingleExecution() {
  val actionName: String = "CollapseAllRegions"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimCollapseRegion : VimActionHandler.SingleExecution() {
  private val actionName: String = "CollapseRegion"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimCollapseRegionRecursively : VimActionHandler.SingleExecution() {
  private val actionName: String = "CollapseRegionRecursively"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimExpandAllRegions : VimActionHandler.SingleExecution() {
  private val actionName: String = "ExpandAllRegions"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimExpandRegion : VimActionHandler.SingleExecution() {
  private val actionName: String = "ExpandRegion"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimExpandRegionRecursively : VimActionHandler.SingleExecution() {
  private val actionName: String = "ExpandRegionRecursively"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}
