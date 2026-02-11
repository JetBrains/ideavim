/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

abstract class VimOutputPanelServiceBase : VimOutputPanelService {
  override fun getOrCreate(editor: VimEditor, context: ExecutionContext): VimOutputPanel {
    return getCurrentOutputPanel() ?: create(editor, context)
  }

  override fun output(editor: VimEditor, context: ExecutionContext, text: String, messageType: MessageType) {
    val panel = getOrCreate(editor, context)
    panel.addText(text, true, messageType)
    panel.show()
  }
}