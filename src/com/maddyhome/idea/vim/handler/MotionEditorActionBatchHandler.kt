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
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command

/**
 * @author Alex Plate
 *
 * Base class for motion handlers.
 * This handler executes an action only once for all carets. That means that if you have 5 carets, [getOffset] will be
 *   called 1 time.
 * @see [MotionEditorActionHandler] for per-caret execution
 */
abstract class MotionEditorActionBatchHandler : MotionEditorActionHandler() {

    override val alwaysBatchExecution = true

    /**
     * This method should return new offset for primary caret
     * It executes once for all carets. That means that if you have 5 carets, [getOffset] will be
     *   called 1 time.
     */
    abstract fun getOffset(editor: Editor, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int

    /**
     * This method is called before [getOffset].
     * The method executes only once.
     */
    protected open fun preOffsetComputation(editor: Editor, context: DataContext, cmd: Command): Boolean = true

    /**
     * This method is called after [getOffset], but before caret motion.
     *
     * The method executes only once.
     */
    protected open fun preMove(editor: Editor, context: DataContext, cmd: Command) = Unit

    /**
     * This method is called after [getOffset] and after caret motion.
     *
     * The method executes only once it there is block selection.
     */
    protected open fun postMove(editor: Editor, context: DataContext, cmd: Command) = Unit

    final override fun getOffset(editor: Editor, caret: Caret, context: DataContext, count: Int, rawCount: Int, argument: Argument?): Int {
        return getOffset(editor, context, count, rawCount, argument)
    }

    final override fun preOffsetComputation(editor: Editor, caret: Caret, context: DataContext, cmd: Command): Boolean {
        return preOffsetComputation(editor, context, cmd)
    }

    final override fun preMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
        return preMove(editor, context, cmd)
    }

    final override fun postMove(editor: Editor, caret: Caret, context: DataContext, cmd: Command) {
        return postMove(editor, context, cmd)
    }
}