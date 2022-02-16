/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.actionSystem.CaretSpecificDataContext
import com.maddyhome.idea.vim.helper.EditorDataContext

/**
 * This would be ideal if we could provide a typed solution, but sofar this is just a holder
 */

interface ExecutionContext {
  val context: Any

  // TODO: 10.02.2022 Not sure about this method
  fun updateEditor(editor: VimEditor): ExecutionContext

  companion object {
    fun onEditor(editor: VimEditor, prevContext: ExecutionContext? = null): ExecutionContext {
      return IjExecutionContext(EditorDataContext.init((editor as IjVimEditor).editor, prevContext?.ij))
    }

    fun onCaret(caret: VimCaret, prevContext: ExecutionContext): ExecutionContext {
      return IjExecutionContext(CaretSpecificDataContext(prevContext.ij, caret.ij))
    }
  }
}

val ExecutionContext.ij: DataContext
  get() = (this as IjExecutionContext).context

class IjExecutionContext(override val context: DataContext) : ExecutionContext {
  override fun updateEditor(editor: VimEditor): ExecutionContext {
    return IjExecutionContext(EditorDataContext.init((editor as IjVimEditor).editor, context))
  }
}

val DataContext.vim
  get() = IjExecutionContext(this)
