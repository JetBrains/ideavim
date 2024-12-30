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
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

fun VimEditor.exitVisualMode() {
  val selectionType = mode.selectionType ?: SelectionType.CHARACTER_WISE
  SelectionVimListenerSuppressor.lock().use {
    if (inBlockSelection) {
      removeSecondaryCarets()
    }
    nativeCarets().forEach(VimCaret::removeSelection)
  }
  if (inVisualMode) {
    vimLastSelectionType = selectionType
    injector.markService.setVisualSelectionMarks(this)
    nativeCarets().forEach { it.vimSelectionStartClear() }

    // We usually want to return to the mode that we were in before we started Visual. Typically, this will be NORMAL,
    // but can be INSERT for "Insert Visual" (`i<C-O>v`). For "Select Visual" (`gh<C-O>`) we can't return to SELECT,
    // because we've just removed the selection. We have to return to NORMAL.
    val mode = this.mode
    this.mode = when {
      mode is Mode.VISUAL && mode.isSelectPending -> Mode.NORMAL()
      else -> mode.returnTo
    }
  }
}
