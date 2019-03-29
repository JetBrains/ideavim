package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimCommandAction
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */

private object SelectEnterActionHandler : EditorActionHandlerBase() {
    override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        SelectionVimListenerSuppressor.lock()
        VimPlugin.getChange().processEnter(InjectedLanguageUtil.getTopLevelEditor(editor), context)
        VimPlugin.getVisualMotion().exitSelectMode(editor)
        VimPlugin.getChange().insertBeforeCursor(editor, context)
        SelectionVimListenerSuppressor.unlock()
        return true
    }
}

class SelectEnterAction : VimCommandAction(SelectEnterActionHandler) {
    override fun getMappingModes(): MutableSet<MappingMode> = MappingMode.S

    override fun getKeyStrokesSet(): MutableSet<MutableList<KeyStroke>> = parseKeysSet("<enter>")

    override fun getType(): Command.Type = Command.Type.INSERT
}