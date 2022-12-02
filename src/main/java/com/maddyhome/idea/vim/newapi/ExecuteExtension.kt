/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.VimVisualPosition
import com.maddyhome.idea.vim.api.injector

fun NativeAction?.execute(context: ExecutionContext) {
  if (this == null) return
  injector.actionExecutor.executeAction(null, this, context)
}

val VisualPosition.vim: VimVisualPosition
  get() = VimVisualPosition(line, column, leansRight)
