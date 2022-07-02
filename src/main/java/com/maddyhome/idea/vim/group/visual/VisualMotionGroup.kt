/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.find.FindManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimVisualMotionGroupBase
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.engine
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

/**
 * @author Alex Plate
 */
class VisualMotionGroup : VimVisualMotionGroupBase() {
  override fun autodetectVisualSubmode(editor: VimEditor): VimStateMachine.SubMode {
    // IJ specific. See https://youtrack.jetbrains.com/issue/VIM-1924.
    val project = editor.ij.project
    if (project != null && FindManager.getInstance(project).selectNextOccurrenceWasPerformed()) {
      return VimStateMachine.SubMode.VISUAL_CHARACTER
    }

    return super.autodetectVisualSubmode(editor)
  }

  /**
   * COMPATIBILITY-LAYER: Added a method
   */
  fun enterVisualMode(editor: Editor, subMode: CommandState.SubMode? = null): Boolean {
    return this.enterVisualMode(editor.vim, subMode?.engine)
  }
}
