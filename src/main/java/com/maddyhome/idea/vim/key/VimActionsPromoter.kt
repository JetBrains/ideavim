/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionWrapper
import com.intellij.openapi.actionSystem.DataContext
import com.maddyhome.idea.vim.action.VimShortcutKeyAction

// VIM-2218
internal class VimActionsPromoter : ActionPromoter {
  override fun promote(actions: MutableList<out AnAction>, context: DataContext): List<AnAction> {
    return actions.filter {
      it is AnActionWrapper && it.delegate is VimShortcutKeyAction
    }
  }
}
