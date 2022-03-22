package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.injector

fun NativeAction?.execute(context: ExecutionContext) {
  if (this == null) return
  injector.actionExecutor.executeAction(this, context)
}
