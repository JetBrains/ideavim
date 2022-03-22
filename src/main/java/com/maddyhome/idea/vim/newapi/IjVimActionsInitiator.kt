package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimActionsInitiator
import com.maddyhome.idea.vim.handler.ActionBeanClass
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase

class IjVimActionsInitiator(val bean: ActionBeanClass) : VimActionsInitiator {
  override fun getInstance(): EditorActionHandlerBase = bean.instance
}

val VimActionsInitiator.ij: ActionBeanClass
  get() = (this as IjVimActionsInitiator).bean