package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.MotionActionHandler
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectMoveDownActionHandler : MotionActionHandler.ForEachCaret() {
    override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
        VimPlugin.getVisualMotion().exitSelectMode(editor, false)
        return VimPlugin.getMotion().moveCaretVertical(editor, caret, count)
    }
}

class SelectMotionDownAction : VimCommandAction(SelectMoveDownActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.S

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<Down>")

    override fun getType(): Command.Type = Command.Type.MOTION
}