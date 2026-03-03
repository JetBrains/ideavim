/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.vfs.VirtualFile

/**
 * Extracted helper methods for file-related operations.
 * Used by listeners, commands, and the frontend module.
 */
object FileGroupHelper {

  /**
   * Respond to editor tab selection and remember the last used tab.
   */
  fun fileEditorManagerSelectionChangedCallback(event: FileEditorManagerEvent) {
    if (event.oldFile != null) {
      LastTabService.getInstance(event.manager.project).lastTab = event.oldFile
    }
  }

  /**
   * Returns the previous tab, or null if none is available.
   * Used by [BufferListCommand][com.maddyhome.idea.vim.vimscript.model.commands.BufferListCommand]
   * to mark the alternate buffer with `#`.
   */
  fun getPreviousTab(context: DataContext): VirtualFile? {
    val project = PlatformDataKeys.PROJECT.getData(context) ?: return null
    val vf = LastTabService.getInstance(project).lastTab
    return if (vf != null && vf.isValid) vf else null
  }
}
