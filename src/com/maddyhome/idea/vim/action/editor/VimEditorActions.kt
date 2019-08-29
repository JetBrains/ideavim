package com.maddyhome.idea.vim.action.editor

import com.maddyhome.idea.vim.action.NativeAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.key.Shortcut
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

class VimEditorBackSpace : NativeAction() {
  override val actionName: String = "EditorBackSpace"

  override val mappingModes: Set<MappingMode> = MappingMode.I

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
  )

  override val type: Command.Type = Command.Type.INSERT
}

class VimEditorDelete : NativeAction() {
  override val actionName: String = "EditorDelete"

  override val mappingModes: Set<MappingMode> = MappingMode.I

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

class VimEditorDown : NativeAction() {
  override val actionName: String = "EditorDown"

  override val mappingModes: Set<MappingMode> = MappingMode.I

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)
}

class VimEditorTab : NativeAction() {
  override val actionName: String = "EditorTab"

  override val mappingModes: Set<MappingMode> = MappingMode.I

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

class VimEditorUp : NativeAction() {
  override val actionName: String = "EditorUp"

  override val mappingModes: Set<MappingMode> = MappingMode.I

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)
}

class VimQuickJavaDoc : NativeAction() {
  override val actionName: String = "QuickJavaDoc"

  override val mappingModes: Set<MappingMode> = MappingMode.N

  override val keyStrokesSet: Set<List<KeyStroke>> = parseKeysSet("K")

  override val type: Command.Type = Command.Type.OTHER_READONLY
}
