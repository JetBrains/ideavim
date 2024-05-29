/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.state.mode.Mode.NORMAL

public abstract class VimProcessGroupBase : VimProcessGroup {
  public override fun cancelExEntry(editor: VimEditor, resetCaret: Boolean) {
    editor.mode = NORMAL()
    injector.commandLine.getActiveCommandLine()?.deactivate(true, resetCaret)
    getInstance().keyHandlerState.leaveCommandLine()
    getInstance().reset(editor)
  }
}
