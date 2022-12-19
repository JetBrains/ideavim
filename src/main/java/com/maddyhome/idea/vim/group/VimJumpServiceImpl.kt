/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpService
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.newapi.IjVimEditor

class VimJumpServiceImpl : VimJumpServiceBase() {
  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    val project = (editor as IjVimEditor).editor.project
    if (project != null) {
      IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
    }
  }
}
