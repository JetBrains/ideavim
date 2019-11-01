package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author Alex Plate
 */
// FIXME: 2019-07-05 Workaround to make jump through methods work
class CtrlDownAction : VimActionHandler.SingleExecution() {

  private val keySet = parseKeysSet("<C-Down>")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val keyStroke = keySet.first().first()
    val actions = VimPlugin.getKey().getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (KeyHandler.executeAction(action, context)) break
    }
    return true
  }
}

class CtrlUpAction : VimActionHandler.SingleExecution() {

  private val keySet = parseKeysSet("<C-Up>")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val keyStroke = keySet.first().first()
    val actions = VimPlugin.getKey().getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (KeyHandler.executeAction(action, context)) break
    }
    return true
  }
}
