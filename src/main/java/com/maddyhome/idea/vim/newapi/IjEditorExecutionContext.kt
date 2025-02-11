/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.maddyhome.idea.vim.api.ExecutionContext

internal open class IjEditorExecutionContext(override val context: DataContext) : ExecutionContext

// This key is stored in data context when the action is started from vim
internal val runFromVimKey = DataKey.create<Boolean>("RunFromVim")

/**
 * Check if the action with this data context was started from Vim
 */
internal val DataContext.actionStartedFromVim: Boolean
  get() = this.getData(runFromVimKey) == true

val DataContext.vim: ExecutionContext
  get() = IjEditorExecutionContext(this)

val ExecutionContext.ij: DataContext
  get() = (this as IjEditorExecutionContext).context
