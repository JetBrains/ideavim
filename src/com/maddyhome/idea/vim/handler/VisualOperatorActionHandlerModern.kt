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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.motion.visualBlockRange
import com.maddyhome.idea.vim.group.motion.visualRange
import com.maddyhome.idea.vim.helper.EditorData

/**
 * @author Alex Plate
 */
abstract class VisualOperatorActionHandlerModern(
        runForEachCaret: Boolean,
        private val caretOrder: CaretOrder
) : VisualOperatorActionHandler(runForEachCaret, caretOrder) {

    constructor() : this(false, CaretOrder.NATIVE)

    protected abstract fun executeCharacterAndLinewise(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: TextRange): Boolean
    protected abstract fun executeBlockwise(editor: Editor, context: DataContext, cmd: Command, ranges: Map<Caret, TextRange>): Boolean

    protected open fun beforeExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    protected open fun beforeCaLExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterCaLExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    protected open fun beforeBlockExecution(editor: Editor, context: DataContext, cmd: Command) = true
    protected open fun afterBlockExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}

    final override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
        logger.debug("execute, cmd=$cmd")

        EditorData.setChangeSwitchMode(editor, null)
        EditorData.setWasVisualBlockMode(editor, CommandState.inVisualBlockMode(editor))

        if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
            val range = editor.visualBlockRange
            logger.debug("range=$range")
        }

        val executeForEachCaretSeparately = !CommandState.inVisualBlockMode(editor)
        val runnable = VisualStartFinishRunnable(editor, cmd, executeForEachCaretSeparately)
        runnable.start()

        if (!beforeExecution(editor, context, cmd)) return false

        val ranges = editor.caretModel.allCarets.associateWith { it.visualRange }

        val res = Ref.create(true)

        if (executeForEachCaretSeparately) {
            if (!beforeCaLExecution(editor, context, cmd)) return false
            editor.caretModel.runForEachCaret({ caret ->
                val range = ranges[caret] ?: run {
                    res.set(false)
                    return@runForEachCaret
                }
                val loopRes = executeCharacterAndLinewise(editor, caret, context, cmd, range)
                res.set(loopRes)
            }, caretOrder == CaretOrder.DECREASING_OFFSET)
            afterCaLExecution(editor, context, cmd, res.get())
        } else {
            if (!beforeBlockExecution(editor, context, cmd)) return false
            val loopRes = executeBlockwise(editor, context, cmd, ranges)
            res.set(loopRes)
            afterBlockExecution(editor, context, cmd, res.get())
        }

        afterExecution(editor, context, cmd, res.get())

        runnable.setRes(res.get())
        runnable.finish()

        EditorData.getChangeSwitchMode(editor)?.let {
            VimPlugin.getChange().processPostChangeModeSwitch(editor, context, it)
        }

        return res.get()
    }

    final override fun execute(editor: Editor, context: DataContext, cmd: Command, range: TextRange) = true
    final override fun execute(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: TextRange) = true

    companion object {
        val logger = Logger.getInstance(VisualOperatorActionHandlerModern::class.java.name)
    }
}