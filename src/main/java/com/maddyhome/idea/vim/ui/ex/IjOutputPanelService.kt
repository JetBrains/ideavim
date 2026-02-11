/*
 * Copyright 2003-2026 The IdeaVim authors
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
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.ui.OutputPanel
import java.lang.ref.WeakReference

class IjOutputPanelService : VimOutputPanelServiceBase() {
  // Weak reference to avoid retaining Editor -> Project after a project is closed,
  // since this service is shared across all IDE projects
  private var activeOutputPanel: WeakReference<VimOutputPanel>? = null

  override fun getCurrentOutputPanel(): VimOutputPanel? {
    return activeOutputPanel?.get()?.takeIf { it.isPanelVisible }
  }

  override fun create(editor: VimEditor, context: ExecutionContext): VimOutputPanel {
    val panel = OutputPanel.getInstance(editor.ij)
    activeOutputPanel = WeakReference(panel)
    return panel
  }
}
