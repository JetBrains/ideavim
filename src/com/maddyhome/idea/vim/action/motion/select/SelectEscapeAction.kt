package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

object SelectEscapeActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        val blockMode = CommandState.inVisualBlockMode(editor)
        VimPlugin.getVisualMotion().exitSelectMode(editor, true)
        if (blockMode) editor.caretModel.removeSecondaryCarets()
        return true
    }
}

class SelectEscapeAction : VimCommandAction(SelectEscapeActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.S

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<esc>")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}