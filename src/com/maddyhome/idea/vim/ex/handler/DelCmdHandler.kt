package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags

class DelCmdHandler : CommandHandler(
        commands("delc[ommand]"),
        flags(Flag.RANGE_FORBIDDEN, Flag.ARGUMENT_REQUIRED)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        if (!VimPlugin.getCommand().hasAlias(cmd.argument)) {
            VimPlugin.showMessage("E184: No such user-defined command: ${cmd.argument}")
            return false
        }

        VimPlugin.getCommand().removeAlias(cmd.argument)
        return true
    }
}