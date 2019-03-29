package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.MotionEditorActionHandler
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectExtendToRightHandler : MotionEditorActionHandler() {
    override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
        return VimPlugin.getMotion().moveCaretHorizontal(editor, caret, count, true)
    }
}

class SelectExtendToRight : VimCommandAction(SelectExtendToRightHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.S

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<S-Right>")

    override fun getType(): Command.Type = Command.Type.MOTION
}