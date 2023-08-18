/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.find.FindManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualMotionGroupBase
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.command.engine
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

/**
 * @author Alex Plate
 */
internal class VisualMotionGroup : VimVisualMotionGroupBase() {
  override fun autodetectVisualSubmode(editor: VimEditor): SelectionType {
    // IJ specific. See https://youtrack.jetbrains.com/issue/VIM-1924.
    val project = editor.ij.project
    if (project != null && FindManager.getInstance(project).selectNextOccurrenceWasPerformed()) {
      return SelectionType.CHARACTER_WISE
    }

    return super.autodetectVisualSubmode(editor)
  }

  /**
   * COMPATIBILITY-LAYER: Added a method
   * Please see: https://jb.gg/zo8n0r
   */
  fun enterVisualMode(editor: Editor, subMode: CommandState.SubMode? = null): Boolean {
    return this.enterVisualMode(editor.vim, subMode?.engine)
  }
}
