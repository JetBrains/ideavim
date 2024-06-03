/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.state.mode.Mode.NORMAL
import javax.swing.KeyStroke

public abstract class VimProcessGroupBase : VimProcessGroup {
  public override fun processExKey(editor: VimEditor, stroke: KeyStroke, processResultBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean {
    // This will only get called if somehow the key focus ended up in the editor while the ex entry window
    // is open. So I'll put focus back in the editor and process the key.
    // FIXME comment above is not true. This method is called all the time. Is there a way to make it work like in the comment above?
    // TODO maybe something like `Propagate.CONTINUE` will help

    val panel = injector.commandLine.getActiveCommandLine()
    if (panel != null) {
      processResultBuilder.addExecutionStep { _, _, _ ->
        panel.focus()
        panel.handleKey(stroke)
      }
      return true
    } else {
      processResultBuilder.addExecutionStep { _, lambdaEditor, _ ->
        lambdaEditor.mode = NORMAL()
        getInstance().reset(lambdaEditor)
      }
      return false
    }
  }

  public override fun cancelExEntry(editor: VimEditor, resetCaret: Boolean) {
    editor.mode = NORMAL()
    injector.commandLine.getActiveCommandLine()?.deactivate(refocusOwningEditor = true, resetCaret)
    getInstance().keyHandlerState.leaveCommandLine()
    getInstance().reset(editor)
  }
}
