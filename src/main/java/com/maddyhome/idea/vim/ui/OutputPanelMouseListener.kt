/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class OutputPanelMouseListener internal constructor() : MouseListener {
  override fun mouseClicked(e: MouseEvent?) {
    val mouseOption = injector.globalOptions().mouse
    if (mouseOption.contains("r") || mouseOption.contains("a")) {
      injector.outputPanel.getCurrentOutputPanel()?.close()
    }
  }

  override fun mousePressed(e: MouseEvent?) {
  }

  override fun mouseReleased(e: MouseEvent?) {
  }

  override fun mouseEntered(e: MouseEvent?) {
  }

  override fun mouseExited(e: MouseEvent?) {
  }

}
