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
