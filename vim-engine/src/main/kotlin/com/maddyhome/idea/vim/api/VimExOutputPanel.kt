/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimExOutputPanelService {
  public fun getPanel(editor: VimEditor): VimExOutputPanel
}

public interface VimExOutputPanel {
  public val text: String?

  public fun output(text: String)
  public fun clear()
}
