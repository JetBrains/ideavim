package com.maddyhome.idea.vim.action.fold

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.VimActionHandler
import javax.swing.KeyStroke

class VimCollapseAllRegions : VimActionHandler.SingleExecution() {
  val actionName: String = "CollapseAllRegions"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zM")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimCollapseRegion : VimActionHandler.SingleExecution() {
  private val actionName: String = "CollapseRegion"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zc")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimCollapseRegionRecursively : VimActionHandler.SingleExecution() {
  private val actionName: String = "CollapseRegionRecursively"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zC")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimExpandAllRegions : VimActionHandler.SingleExecution() {
  private val actionName: String = "ExpandAllRegions"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zR")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimExpandRegion : VimActionHandler.SingleExecution() {
  private val actionName: String = "ExpandRegion"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zo")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimExpandRegionRecursively : VimActionHandler.SingleExecution() {
  private val actionName: String = "ExpandRegionRecursively"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zO")

  override val type: Command.Type = Command.Type.OTHER_READONLY
  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}
