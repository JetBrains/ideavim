/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper

/**
 * @author Alex Plate
 */
abstract class MotionEditorActionHandler : EditorActionHandlerBase(false) {

    abstract fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int

    protected open fun preMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean = true
    protected open fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) = Unit

    protected open val alwaysBatchExecution = false

    final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        val visualBlockActive = CommandState.inVisualBlockMode(editor)

        return if (visualBlockActive || editor.caretModel.caretCount == 1 || alwaysBatchExecution) {
            val primaryCaret = editor.caretModel.primaryCaret
            doExecute(editor, primaryCaret, context, cmd)
        } else {
            val resHolder = Ref.create(true)
            editor.caretModel.runForEachCaret { caret ->
                resHolder.set(resHolder.get() && doExecute(editor, caret, context, cmd))
            }
            resHolder.get()
        }
    }

    private fun doExecute(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
        var offset = getOffset(editor, caret, context, cmd.count, cmd.rawCount, cmd.argument)

        if (!preMove(editor, caret, context, cmd)) return false

        return when {
            offset == -1 -> false
            offset >= 0 -> {
                if (CommandFlags.FLAG_SAVE_JUMP in cmd.flags) {
                    VimPlugin.getMark().saveJumpLocation(editor)
                }
                if (!CommandState.inInsertMode(editor) &&
                        !CommandState.inRepeatMode(editor) &&
                        !CommandState.inVisualCharacterMode(editor)) {
                    offset = EditorHelper.normalizeOffset(editor, offset, false)
                }
                MotionGroup.moveCaret(editor, caret, offset)
                postMove(editor, caret, context, cmd)
                true
            }
            else -> true
        }
    }

    final override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command) = true
}