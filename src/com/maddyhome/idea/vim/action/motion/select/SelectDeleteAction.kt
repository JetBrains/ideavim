package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectDeleteActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
        val actions = VimPlugin.getKey().getActions(editor.component, enterKeyStroke)
        for (action in actions) {
            if (KeyHandler.executeAction(action, context)) {
                break
            }
        }
        VimPlugin.getVisualMotion().exitSelectMode(editor, true)
        return true
    }
}

class SelectDeleteAction : VimCommandAction(SelectDeleteActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.S

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<BS>", "<DEL>")

    override fun getType(): Command.Type = Command.Type.INSERT
}