package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags

class NextTabHandler : CommandHandler(
        commands { +"tabn" withOptional "ext" },
        flags(ARGUMENT_OPTIONAL, RANGE_FORBIDDEN)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        VimPlugin.getMotion().moveCaretGotoNextTab(editor, context, cmd.argument.toIntOrNull() ?: 0)
        return true
    }
}
