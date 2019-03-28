package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.CaretVimListenerSuppressor
import com.maddyhome.idea.vim.group.ChangeGroup
import com.maddyhome.idea.vim.group.motion.vimSetSelectionSilently
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.vimSelectionStart
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectEnableModeActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        CommandState.getInstance(editor).pushState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.SELECT)
        CaretVimListenerSuppressor.lock()
        editor.caretModel.primaryCaret.run {
            vimSetSelectionSilently(offset, offset + 1)
            vimSelectionStart = offset
            moveToOffset(offset + 1)
        }
        ChangeGroup.resetCursor(editor, true)
        CaretVimListenerSuppressor.unlock()
        return true
    }
}

class SelectEnableModeAction : VimCommandAction(SelectEnableModeActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.N

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("gh")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}