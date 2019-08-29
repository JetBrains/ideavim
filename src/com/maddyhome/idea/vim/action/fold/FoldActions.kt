package com.maddyhome.idea.vim.action.fold

import com.maddyhome.idea.vim.action.NativeAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import javax.swing.KeyStroke

class VimCollapseAllRegions : NativeAction() {
  override val actionName: String = "CollapseAllRegions"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zM")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}

class VimCollapseRegion : NativeAction() {
  override val actionName: String = "CollapseRegion"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zc")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}

class VimCollapseRegionRecursively : NativeAction() {
  override val actionName: String = "CollapseRegionRecursively"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zC")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}

class VimExpandAllRegions : NativeAction() {
  override val actionName: String = "ExpandAllRegions"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zR")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}

class VimExpandRegion : NativeAction() {
  override val actionName: String = "ExpandRegion"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zo")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}

class VimExpandRegionRecursively : NativeAction() {
  override val actionName: String = "ExpandRegionRecursively"

  override val mappingModes: Set<MappingMode> = MappingMode.NV

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("zO")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}
