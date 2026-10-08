/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.multiplecursors.enterCharacterwiseVisualMode
import com.maddyhome.idea.vim.extension.multiplecursors.leaveVisualMode
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.state.mode.inVisualMode

/**
 * Switches between the regions, which are Visual selections, and the cursors in Normal mode
 *
 * Like vim-visual-multi, a region becomes a cursor where its selection started, and a cursor becomes a region of one
 * character.
 */
internal class SwitchModeHandler : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    if (editor.inVisualMode) {
      regionsToCursors(editor)
    } else {
      cursorsToRegions(editor)
    }
  }

  private fun cursorsToRegions(editor: VimEditor) {
    enterCharacterwiseVisualMode(editor)
    editor.nativeCarets().forEach { it.vimSetSelection(it.offset) }
  }

  private fun regionsToCursors(editor: VimEditor) {
    val selectionStarts = editor.nativeCarets().associateWith { it.vimSelectionStart }
    leaveVisualMode(editor)
    selectionStarts.forEach { (caret, start) -> caret.moveToOffset(start) }
  }
}
