package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.ChangeGroup
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup.selectionAdj
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectToggleVisualModeHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        val commandState = CommandState.getInstance(editor)
        val subMode = commandState.subMode
        val mode = commandState.mode
        commandState.popState()
        if (mode == CommandState.Mode.VISUAL) {
            commandState.pushState(CommandState.Mode.SELECT, subMode, MappingMode.SELECT)
            editor.caretModel.runForEachCaret {
                it.moveToOffset(it.offset + selectionAdj)
            }
        } else {
            commandState.pushState(CommandState.Mode.VISUAL, subMode, MappingMode.VISUAL)
            editor.caretModel.runForEachCaret {
                it.moveToOffset(it.selectionEnd - selectionAdj)
            }
        }
        ChangeGroup.resetCursor(editor, mode == CommandState.Mode.VISUAL)
        return true
    }
}

class SelectToggleVisualMode : VimCommandAction(SelectToggleVisualModeHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.VS

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<C-G>")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}