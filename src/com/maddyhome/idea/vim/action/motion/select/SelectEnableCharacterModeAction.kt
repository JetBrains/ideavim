package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup
import com.maddyhome.idea.vim.group.motion.vimSetSelectionSilently
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectEnableCharacterModeActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        editor.caretModel.primaryCaret.run {
            vimSetSelectionSilently(offset, offset + 1)
            moveToOffset(offset + 1)
        }
        return VisualMotionGroup.enterSelectionMode(editor, CommandState.SubMode.VISUAL_CHARACTER)
    }
}

class SelectEnableCharacterModeAction : VimCommandAction(SelectEnableCharacterModeActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.N

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("gh")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}