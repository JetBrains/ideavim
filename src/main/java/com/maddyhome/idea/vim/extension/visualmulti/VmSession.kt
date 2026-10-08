/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.extension.multiplecursors.isGlobalFlagEnabled
import com.maddyhome.idea.vim.helper.userData
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inVisualMode

private var Editor.vimVisualMultiSession: Boolean? by userData()

/**
 * The multi-cursor session of an editor, like `b:visual_multi` in vim-visual-multi
 *
 * The session starts with the first region, and ends with Exit or when the last region is removed.
 */
internal object VmSession {

  /**
   * vim-visual-multi keeps its own regions, while ours are the carets and their selections. The IDE can remove them,
   * so they are checked as well.
   */
  fun isActive(editor: VimEditor): Boolean = editor.ij.vimVisualMultiSession == true && hasRegions(editor)

  fun startIfRegionsCreated(editor: VimEditor) {
    if (hasRegions(editor)) {
      editor.ij.vimVisualMultiSession = true
    }
  }

  fun end(editor: VimEditor) {
    editor.ij.vimVisualMultiSession = null
  }

  /**
   * [VimEditor.carets] counts the carets of Visual block mode as one, so it is not a session
   */
  private fun hasRegions(editor: VimEditor): Boolean = editor.inVisualMode || editor.carets().size > 1

  /**
   * Ends the session when the mode changes with at most one cursor, e.g. after removing the last region or leaving
   * Visual mode with `v`.
   *
   * vim-visual-multi keeps a single cursor in Normal mode until it is exited. We don't highlight the regions, so such a
   * session would be invisible, and `n` would unexpectedly behave differently.
   *
   * With `g:VM_quit_after_leaving_insert_mode`, leaving Insert mode exits the session too.
   */
  object EndOnModeChange : ModeChangeListener {
    override fun modeChanged(editor: VimEditor, oldMode: Mode) {
      if (editor.carets().size <= 1) {
        end(editor)
      } else if (oldMode is Mode.INSERT && isActive(editor) && quitsAfterInsertMode()) {
        end(editor)
        editor.removeSecondaryCarets()
      }
    }

    private fun quitsAfterInsertMode() = isGlobalFlagEnabled("VM_quit_after_leaving_insert_mode", default = false)
  }
}
