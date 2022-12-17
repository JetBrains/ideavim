/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor

fun VimEditor.exitVisualMode() {
  val selectionType = SelectionType.fromSubMode(this.subMode)
  SelectionVimListenerSuppressor.lock().use {
    if (inBlockSubMode) {
      this.removeSecondaryCarets()
    }
    if (!this.vimKeepingVisualOperatorAction) {
      this.nativeCarets().forEach(VimCaret::removeSelection)
    }
  }
  if (this.inVisualMode) {
    this.vimLastSelectionType = selectionType
    injector.markService.setVisualSelectionMarks(this)
    this.nativeCarets().forEach { it.vimSelectionStartClear() }

    this.vimStateMachine.popModes()
  }
}
