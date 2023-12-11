/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnTo
import com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.mode
import com.maddyhome.idea.vim.state.mode.returnTo
import com.maddyhome.idea.vim.state.mode.selectionType

public fun VimEditor.exitVisualMode() {
  val selectionType = this.mode.selectionType ?: CHARACTER_WISE
  SelectionVimListenerSuppressor.lock().use {
    if (inBlockSelection) {
      this.removeSecondaryCarets()
    }
    this.nativeCarets().forEach(VimCaret::removeSelection)
  }
  if (this.inVisualMode) {
    this.vimLastSelectionType = selectionType
    injector.markService.setVisualSelectionMarks(this)
    this.nativeCarets().forEach { it.vimSelectionStartClear() }

    val returnTo = this.vimStateMachine.mode.returnTo
    when (returnTo) {
      ReturnTo.INSERT -> {
        this.vimStateMachine.mode = Mode.INSERT
      }

      ReturnTo.REPLACE -> {
        this.vimStateMachine.mode = Mode.REPLACE
      }

      null -> {
        this.vimStateMachine.mode = Mode.NORMAL()
      }
    }
  }
}
