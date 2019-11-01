package com.maddyhome.idea.vim.action.editor

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

class VimEditorBackSpace : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  private val actionName: String = "EditorBackSpace"

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimEditorDelete : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  private val actionName: String = "EditorDelete"

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimEditorDown : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  private val actionName: String = "EditorDown"

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimEditorTab : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  private val actionName: String = "EditorTab"

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimEditorUp : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  private val actionName: String = "EditorUp"

  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
  )

  override val type: Command.Type = Command.Type.INSERT

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}

class VimQuickJavaDoc : VimActionHandler.SingleExecution() {
  private val actionName: String = "QuickJavaDoc"

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.executeAction(actionName, context)
    return true
  }
}
