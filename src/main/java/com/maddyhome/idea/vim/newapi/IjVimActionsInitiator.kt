/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimActionsInitiator
import com.maddyhome.idea.vim.handler.ActionBeanClass
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase

class IjVimActionsInitiator(val bean: ActionBeanClass) : VimActionsInitiator {
  override fun getInstance(): EditorActionHandlerBase = bean.instance
}

val VimActionsInitiator.ij: ActionBeanClass
  get() = (this as IjVimActionsInitiator).bean
