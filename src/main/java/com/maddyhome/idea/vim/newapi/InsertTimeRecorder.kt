/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService

internal class InsertTimeRecorder : ModeChangeListener {
  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    editor as IjVimEditor
    if (oldMode == Mode.INSERT) {
      val undo = injector.undo as? VimTimestampBasedUndoService ?: return
      val nanoTime = System.nanoTime()
      injector.application.runReadAction {
        editor.nativeCarets().forEach { undo.endInsertSequence(it, it.offset, nanoTime) }
      }
    }
  }
}
