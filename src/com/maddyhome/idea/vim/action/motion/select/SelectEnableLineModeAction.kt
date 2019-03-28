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
import com.maddyhome.idea.vim.helper.EditorHelper
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectEnableLineModeActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        editor.caretModel.runForEachCaret { caret ->
            val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
            val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
            caret.vimSetSelectionSilently(lineStart, lineEnd)
        }
        return VisualMotionGroup.enterSelectionMode(editor, CommandState.SubMode.VISUAL_LINE)
    }
}

class SelectEnableLineModeAction : VimCommandAction(SelectEnableLineModeActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.N

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("gH")

    override fun getType(): Command.Type = Command.Type.OTHER_READONLY
}