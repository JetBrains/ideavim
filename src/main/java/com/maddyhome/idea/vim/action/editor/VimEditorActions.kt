/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.editor

import com.intellij.openapi.actionSystem.IdeActions
import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.IdeActionHandler
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

internal class VimEditorBackSpace : IdeActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
  )
  override val type: Command.Type = Command.Type.DELETE
}

internal class VimEditorDelete : IdeActionHandler(IdeActions.ACTION_EDITOR_DELETE), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
  )
  override val type: Command.Type = Command.Type.DELETE
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

internal class VimEditorDown : IdeActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
  )
  override val type: Command.Type = Command.Type.MOTION
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)
}

internal class VimEditorTab : IdeActionHandler(IdeActions.ACTION_EDITOR_TAB), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
  )
  override val type: Command.Type = Command.Type.INSERT
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_STROKE)
}

internal class VimEditorUp : IdeActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> = setOf(
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    listOf(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
  )
  override val type: Command.Type = Command.Type.MOTION
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_CLEAR_STROKES)
}

internal class VimQuickJavaDoc : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    injector.actionExecutor.executeAction(IdeActions.ACTION_QUICK_JAVADOC, context)
    return true
  }
}
