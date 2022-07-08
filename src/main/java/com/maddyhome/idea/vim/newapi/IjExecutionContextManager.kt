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

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.actionSystem.CaretSpecificDataContext
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ExecutionContextManagerBase
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.EditorDataContext

@Service
class IjExecutionContextManager : ExecutionContextManagerBase() {
  override fun onEditor(editor: VimEditor, prevContext: ExecutionContext?): ExecutionContext {
    return IjExecutionContext(EditorDataContext.init((editor as IjVimEditor).editor, prevContext?.ij))
  }

  override fun onCaret(caret: VimCaret, prevContext: ExecutionContext): ExecutionContext {
    return IjExecutionContext(CaretSpecificDataContext.create(prevContext.ij, caret.ij))
  }

  override fun createCaretSpecificDataContext(context: ExecutionContext, caret: VimCaret): ExecutionContext {
    return IjExecutionContext(CaretSpecificDataContext.create(context.ij, caret.ij))
  }

  override fun createEditorDataContext(editor: VimEditor, context: ExecutionContext): ExecutionContext {
    return EditorDataContext.init(editor.ij, context.ij).vim
  }
}
