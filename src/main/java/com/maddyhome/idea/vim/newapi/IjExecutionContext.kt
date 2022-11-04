/*
 * Copyright 2003-2022 The IdeaVim authors
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

class IjExecutionContext(override val context: DataContext) : ExecutionContext {
  override fun updateEditor(editor: VimEditor): ExecutionContext {
    return IjExecutionContext(EditorDataContext.init((editor as IjVimEditor).editor, context))
  }
}

val DataContext.vim
  get() = IjExecutionContext(this)

val ExecutionContext.ij: DataContext
  get() = (this as IjExecutionContext).context
