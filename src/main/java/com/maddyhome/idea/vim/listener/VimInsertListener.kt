/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode

@Deprecated(
  message = "Please use ModeChangeListener",
  replaceWith = ReplaceWith("ModeChangeListener", imports = ["import com.maddyhome.idea.vim.common.ModeChangeListener"])
)
interface VimInsertListener : ModeChangeListener {
  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    if (editor.mode == Mode.INSERT) {
      insertModeStarted(editor.ij)
    }
  }

  fun insertModeStarted(editor: Editor)
}
