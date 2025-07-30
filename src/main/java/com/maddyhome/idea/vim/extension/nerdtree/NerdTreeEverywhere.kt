/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.components.Service

internal class NerdTreeEverywhere {
  @Service
  class Dispatcher : AbstractDispatcher(mappings) {
    init {
      templatePresentation.isEnabledInModalContext = true

      mappings.registerNavigationMappings()
    }
  }

  companion object {
    val mappings = Mappings("NerdTreeEverywhere")
  }
}
