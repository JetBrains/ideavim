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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.helper.VimSelection

/**
 * @author Alex Plate
 *
 * Base class for visual operation handlers.
 * This handler executes an action only once for all carets. That means that if you have 5 carets,
 *   [executeAction] will be called 1 time.
 * @see [VisualOperatorActionHandler] for per-caret execution
 */
abstract class VisualOperatorActionBatchHandler : VisualOperatorActionHandler() {
    /**
     * Execute an action
     * [caretsAndSelections] contains a map of all current carets and corresponding selections.
     *   If there is block selection, only one caret is in [caretsAndSelections].
     *
     * This method is executed once for all carets.
     */
    abstract fun executeForAllCarets(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>): Boolean

    final override fun executeAction(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: VimSelection) = true

    final override fun beforeExecution(editor: Editor, context: DataContext, cmd: Command, caretsAndSelections: Map<Caret, VimSelection>): Boolean {
        return executeForAllCarets(editor, context, cmd, caretsAndSelections)
    }

    final override fun afterExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) {}
}