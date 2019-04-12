package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.codeInsight.template.TemplateManager
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

private object SelectMoveLeftActionHandler : MotionEditorActionHandler() {
    override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
        VimPlugin.getVisualMotion().exitSelectMode(editor, false)
        TemplateManager.getInstance(editor.project)
                .getActiveTemplate(editor)?.run { VimPlugin.getChange().insertBeforeCursor(editor, context) }
        return VimPlugin.getMotion().moveCaretHorizontal(editor, caret, -count, false)
    }
}

class SelectMotionLeftAction : VimCommandAction(SelectMoveLeftActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.S

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<Left>")

    override fun getType(): Command.Type = Command.Type.MOTION
}