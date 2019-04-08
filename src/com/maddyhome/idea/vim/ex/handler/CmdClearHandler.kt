package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags

class
CmdClearHandler : CommandHandler(
        commands("comc[lear]"),
        flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_FORBIDDEN)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        VimPlugin.getCommand().resetAliases()
        return true
    }
}