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
import com.intellij.openapi.editor.RangeMarker
import com.maddyhome.idea.vim.command.Command

/**
 * @author Alex Plate
 */
abstract class VisualOperatorActionBatchHandler : VisualOperatorActionHandler() {

    abstract fun executeBatch(editor: Editor, context: DataContext, cmd: Command, ranges: Map<Caret, RangeMarker>): Boolean

    final override val operateCaretsInAlwaysBatch: Boolean = true

    final override fun beforeCaLExecution(editor: Editor, context: DataContext, cmd: Command) = true
    final override fun afterCaLExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) = Unit
    final override fun beforeBlockExecution(editor: Editor, context: DataContext, cmd: Command) = true
    final override fun afterBlockExecution(editor: Editor, context: DataContext, cmd: Command, res: Boolean) = Unit

    final override fun executeCharacterAndLinewise(editor: Editor, caret: Caret, context: DataContext, cmd: Command, range: RangeMarker) = true

    final override fun executeBlockwise(editor: Editor, context: DataContext, cmd: Command, ranges: Map<Caret, RangeMarker>): Boolean {
        return executeBatch(editor, context, cmd, ranges)
    }
}