/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
import com.intellij.openapi.actionSystem.IdeActions.ACTION_COLLAPSE_ALL_REGIONS
import com.intellij.openapi.actionSystem.IdeActions.ACTION_COLLAPSE_REGION
import com.intellij.openapi.actionSystem.IdeActions.ACTION_COLLAPSE_REGION_RECURSIVELY
import com.intellij.openapi.actionSystem.IdeActions.ACTION_EXPAND_ALL_REGIONS
import com.intellij.openapi.actionSystem.IdeActions.ACTION_EXPAND_REGION
import com.intellij.openapi.actionSystem.IdeActions.ACTION_EXPAND_REGION_RECURSIVELY
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler

class VimCollapseAllRegions : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(ACTION_COLLAPSE_ALL_REGIONS, context)
    return true
  }
}

class VimCollapseRegion : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(ACTION_COLLAPSE_REGION, context)
    return true
  }
}

class VimCollapseRegionRecursively : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(ACTION_COLLAPSE_REGION_RECURSIVELY, context)
    return true
  }
}

class VimExpandAllRegions : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(ACTION_EXPAND_ALL_REGIONS, context)
    return true
  }
}

class VimExpandRegion : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(ACTION_EXPAND_REGION, context)
    return true
  }
}

class VimExpandRegionRecursively : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(ACTION_EXPAND_REGION_RECURSIVELY, context)
    return true
  }
}
