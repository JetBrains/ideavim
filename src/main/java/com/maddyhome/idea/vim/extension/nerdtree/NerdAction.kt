/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project

sealed class NerdAction {
  class ToIj(val name: String) : NerdAction()
  class Code(val action: (Project, DataContext, AnActionEvent) -> Unit) : NerdAction()
}
