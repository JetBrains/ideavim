/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class LastTabService(val project: Project) {

  var lastTab: VirtualFile? = null

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<LastTabService>()
  }
}
