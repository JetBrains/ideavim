/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEnabler

class IjVimEnabler : VimEnabler {
  private var isNewUser = false

  override fun isEnabled(): Boolean {
    return VimPlugin.isEnabled()
  }

  override fun isNewIdeaVimUser(): Boolean = isNewUser

  fun ideOpened() {
    val myFirstVersion = VimPlugin.getVimState().firstIdeaVimVersion
    if (myFirstVersion == null) {
      VimPlugin.getVimState().firstIdeaVimVersion = VimPlugin.getVersion()
      this.isNewUser = true
    }
  }
}
