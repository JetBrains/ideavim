/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.autocmd.AutoCmdEvent

/**
 * Fires FocusGained/FocusLost autocmd events when the IDE window gains or loses OS-level focus.
 * This matches Vim's behavior where these events fire on application-level focus changes (e.g., alt-tab),
 * not on editor-level focus changes within the IDE.
 */
class VimAppActivationListener : ApplicationActivationListener {

  override fun applicationActivated(ideFrame: IdeFrame) {
    if (VimPlugin.isNotEnabled()) return
    injector.autoCmd.handleEvent(AutoCmdEvent.FocusGained)
  }

  override fun applicationDeactivated(ideFrame: IdeFrame) {
    if (VimPlugin.isNotEnabled()) return
    injector.autoCmd.handleEvent(AutoCmdEvent.FocusLost)
  }
}
