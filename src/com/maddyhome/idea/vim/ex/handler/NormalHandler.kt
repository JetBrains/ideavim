package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.handler.CaretOrder
import com.maddyhome.idea.vim.handler.ExecuteMethodNotOverriddenException
import com.maddyhome.idea.vim.helper.EditorHelper

import javax.swing.*

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys

class NormalHandler : CommandHandler(
        commands("norm[al]"),
        flags(Flag.RANGE_OPTIONAL, Flag.ARGUMENT_REQUIRED, Flag.WRITABLE, Flag.SAVE_VISUAL_MODE),
        true, CaretOrder.INCREASING_OFFSET
) {

    @Throws(ExException::class, ExecuteMethodNotOverriddenException::class)
    override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: ExCommand): Boolean {
        var argument = cmd.argument
        var useMapping = true
        if (!argument.isEmpty() && argument[0] == '!') {
            // Disable mapping by "!" option
            useMapping = false
            argument = argument.substring(1).trim()
        }

        // True if line range was explicitly defined by user
        val rangeUsed = cmd.ranges.size() != 0

        val range = cmd.getLineRange(editor, caret, context)

        val commandState = CommandState.getInstance(editor)
        if (commandState.mode == CommandState.Mode.VISUAL) {
            // Disable visual mode before command execution
            // Otherwise commands will be applied to selected text
            VimPlugin.getMotion().exitVisual(editor)
        }

        for (line in range.startLine..range.endLine) {
            if (rangeUsed) {
                // Move caret to the first position on line
                if (editor.document.lineCount < line) {
                    break
                }
                val startOffset = EditorHelper.getLineStartOffset(editor, line)
                editor.caretModel.moveToOffset(startOffset)
            }

            // Perform operations
            val keys = parseKeys(argument)
            val keyHandler = KeyHandler.getInstance()
            keyHandler.reset(editor)
            for (key in keys) {
                keyHandler.handleKey(editor, key, context, useMapping)
            }

            // Exit if state leaves as insert or cmd_line
            val mode = commandState.mode
            if (mode == CommandState.Mode.EX_ENTRY) {
                VimPlugin.getProcess().cancelExEntry(editor, context)
            }
            if (mode == CommandState.Mode.INSERT || mode == CommandState.Mode.REPLACE) {
                VimPlugin.getChange().processEscape(editor, context)
            }
        }
        return true
    }
}
