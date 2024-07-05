/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.VimOutputPanelServiceBase
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.newapi.ij
import java.lang.ref.WeakReference

class IjOutputPanelService : VimOutputPanelServiceBase() {
  private var activeOutputPanel: VimOutputPanel? = null

  override fun getCurrentOutputPanel(): VimOutputPanel? {
    return activeOutputPanel?.takeIf {
      (it as ExOutputModel)
      it.isActive && it.editor != null
    }
  }

  override fun create(editor: VimEditor, context: ExecutionContext): VimOutputPanel {
    val panel = ExOutputModel(WeakReference(editor.ij))
    activeOutputPanel = panel
    return panel
  }
}