/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ExecutionContextManagerBase
import com.maddyhome.idea.vim.api.VimEditor

@Service
internal class IjExecutionContextManager : ExecutionContextManagerBase() {
  override fun getEditorExecutionContext(editor: VimEditor): ExecutionContext {
    return EditorUtil.getEditorDataContext(editor.ij).vim
  }
}
