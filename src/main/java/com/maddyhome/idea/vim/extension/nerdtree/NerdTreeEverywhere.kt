/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.components.Service
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

internal class NerdTreeEverywhere {
  @Service
  class Dispatcher : AbstractDispatcher(mappings) {
    init {
      templatePresentation.isEnabledInModalContext = true

      mappings.registerNavigationMappings()
      mappings.register("NERDTreeMapActivateNode", "o", Mappings.Action { _, tree ->
        // TODO a more reliable way of invocation (such as double-clicking?)
        val listener = tree.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
        listener.actionPerformed(ActionEvent(tree, ActionEvent.ACTION_PERFORMED, null))
      })
    }
  }

  companion object {
    val mappings = Mappings("NerdTreeEverywhere")
  }
}
