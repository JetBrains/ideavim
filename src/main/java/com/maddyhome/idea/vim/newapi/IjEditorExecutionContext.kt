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
import com.maddyhome.idea.vim.api.injector

internal open class IjEditorExecutionContext(override val context: DataContext) : ExecutionContext.Editor {
  override fun updateEditor(editor: VimEditor): ExecutionContext {
    return IjEditorExecutionContext(injector.executionContextManager.onEditor(editor, context.vim).ij)
  }
}

internal class IjCaretAndEditorExecutionContext(override val context: DataContext) : IjEditorExecutionContext(context), ExecutionContext.CaretAndEditor

/**
 * Data context that defines that some action was started from IdeaVim.
 * You can call use [runFromVimKey] key to define if intellij action was started from IdeaVim
 */
internal class VimDataContext(private val delegate: DataContext) : DataContext {
  override fun getData(dataId: String): Any? {
    if (dataId == runFromVimKey) return true
    return delegate.getData(dataId)
  }
}

internal const val runFromVimKey = "RunFromVim"

public val DataContext.vim: ExecutionContext
  get() = IjEditorExecutionContext(this)

public val ExecutionContext.ij: DataContext
  get() = (this as IjEditorExecutionContext).context
