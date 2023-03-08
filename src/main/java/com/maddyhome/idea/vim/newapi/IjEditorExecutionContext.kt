/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.actionSystem.DataContext
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.EditorDataContext

open class IjEditorExecutionContext(override val context: DataContext) : ExecutionContext.Editor {
  override fun updateEditor(editor: VimEditor): ExecutionContext {
    return IjEditorExecutionContext(EditorDataContext.init((editor as IjVimEditor).editor, context))
  }
}

class IjCaretAndEditorExecutionContext(override val context: DataContext) : IjEditorExecutionContext(context), ExecutionContext.CaretAndEditor

val DataContext.vim: ExecutionContext
  get() = IjEditorExecutionContext(this)

val ExecutionContext.ij: DataContext
  get() = (this as IjEditorExecutionContext).context
