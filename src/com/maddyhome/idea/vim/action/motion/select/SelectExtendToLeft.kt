package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.handler.MotionEditorActionBatchHandler
import java.util.*
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectExtendToLeftHandler : MotionEditorActionBatchHandler() {
    override fun getOffset(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
        return (editor.caretModel.primaryCaret.offset - 1).coerceAtLeast(0)
    }
}

class SelectExtendToLeft : VimCommandAction(SelectExtendToLeftHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = EnumSet.of(MappingMode.SELECT)

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<S-Left>")

    override fun getType(): Command.Type = Command.Type.MOTION
}