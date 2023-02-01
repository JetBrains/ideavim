/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.PotemkinProgress
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.MessageHelper.message
import com.maddyhome.idea.vim.macro.VimMacroBase
import com.maddyhome.idea.vim.newapi.IjVimEditor

/**
 * Used to handle playback of macros
 */
class MacroGroup : VimMacroBase() {
  /**
   * This puts a single keystroke at the end of the event queue for playback
   */
  override fun playbackKeys(
    editor: VimEditor,
    context: ExecutionContext,
    total: Int,
  ) {
    val project = (editor as IjVimEditor).editor.project
    val keyStack = getInstance().keyStack
    if (!keyStack.hasStroke()) {
      logger.debug("done")
      keyStack.removeFirst()
      return
    }
    val potemkinProgress = PotemkinProgress(
      message("progress.title.macro.execution"), project, null,
      message("stop")
    )
    potemkinProgress.isIndeterminate = false
    potemkinProgress.fraction = 0.0
    potemkinProgress.runInSwingThread {

      // Handle one keystroke then queue up the next key
      for (i in 0 until total) {
        potemkinProgress.fraction = (i + 1).toDouble() / total
        while (keyStack.hasStroke()) {
          val key = keyStack.feedStroke()
          try {
            potemkinProgress.checkCanceled()
          } catch (e: ProcessCanceledException) {
            return@runInSwingThread
          }
          ProgressManager.getInstance()
            .executeNonCancelableSection { getInstance().handleKey(editor, key, context) }
        }
        keyStack.resetFirst()
      }
      keyStack.removeFirst()
    }
  }

  companion object {
    private val logger = logger<MacroGroup>()
  }
}
