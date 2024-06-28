/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimExOutputPanelService {
  fun getPanel(editor: VimEditor): VimExOutputPanel
}

interface VimExOutputPanel {
  val isActive: Boolean

  val text: String?

  fun output(text: String)
  fun clear()
  fun close()
}
