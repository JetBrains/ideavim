package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.visual.vimSetSelectionSilently
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectEnableBlockModeActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        editor.caretModel.removeSecondaryCarets()
        editor.caretModel.primaryCaret.run {
            vimSetSelectionSilently(offset, offset + 1)
            moveToOffset(offset + 1)
        }
        return VimPlugin.getVisualMotion().enterSelectionMode(editor, CommandState.SubMode.VISUAL_BLOCK)
    }
}

class SelectEnableBlockModeAction : VimCommandAction(SelectEnableBlockModeActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.N

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("g<C-h>")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}