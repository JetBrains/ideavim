/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEnabler

@Service
internal class IjVimEnabler : VimEnabler {
  private var isNewUser = false

  override fun isEnabled(): Boolean {
    return VimPlugin.isEnabled()
  }

  override fun isNewIdeaVimUser(): Boolean = isNewUser

  fun ideOpened() {
    val myFirstVersion = VimPlugin.getVimState().firstIdeaVimVersion
    if (myFirstVersion == "-1") {
      VimPlugin.getVimState().firstIdeaVimVersion = VimPlugin.getVersion()
      this.isNewUser = true
    }
  }
}
