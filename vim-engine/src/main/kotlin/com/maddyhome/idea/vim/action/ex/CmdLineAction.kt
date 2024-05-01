/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.state.mode.inVisualMode

public interface CmdLineAction {
  public fun getRange(editor: VimEditor, cmd: Command): String {
    var initText = ""
    if (editor.inVisualMode) {
      initText = "'<,'>"
    } else if (cmd.rawCount > 0) {
      initText = if (cmd.count == 1) {
        "."
      } else {
        ".,.+" + (cmd.count - 1)
      }
    }
    return initText
  }
}
