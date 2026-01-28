/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MessageType
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOutputPanel
import com.maddyhome.idea.vim.api.VimOutputPanelService
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.ui.OutputPanel

class IjOutputPanelService : VimOutputPanelService {
  private var activeOutputPanel: VimOutputPanel? = null

  override fun getCurrentOutputPanel(): VimOutputPanel? {
    return activeOutputPanel?.takeIf { it.isActive }
  }


  override fun getOrCreate(editor: VimEditor, context: ExecutionContext): VimOutputPanel {
    return getCurrentOutputPanel() ?: create(editor, context)
  }

  override fun output(editor: VimEditor, context: ExecutionContext, text: String, messageType: MessageType) {
    val panel = getOrCreate(editor, context)
    panel.addText(text, color = messageType.color)
    panel.show()
  }

  fun create(editor: VimEditor, context: ExecutionContext): VimOutputPanel {
    val panel = OutputPanel.getInstance(editor.ij)
    activeOutputPanel = panel
    return panel
  }
}
