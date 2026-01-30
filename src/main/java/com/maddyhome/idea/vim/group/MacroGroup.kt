/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.codeInsight.completion.CompletionPhase
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.PotemkinProgress
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.macro.VimMacroBase
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim

/**
 * Used to handle playback of macros
 */

class MacroGroup : VimMacroBase() {

  // If it's null, this is the top macro (as in most cases). If it's not null, this macro is executed from top macro
  private var potemkinProgress: PotemkinProgress? = null

  /**
   * This puts a single keystroke at the end of the event queue for playback
   */
  override fun playbackKeys(
    editor: VimEditor,
    context: ExecutionContext,
    total: Int,
  ) {
    val project = editor.ij.project ?: return
    val keyStack = getInstance().keyStack
    if (!keyStack.hasStroke()) {
      logger.debug("done")
      keyStack.removeFirst()
      return
    }
    val isInternalMacro = potemkinProgress != null

    val myPotemkinProgress = potemkinProgress ?: PotemkinProgress(
      MessageHelper.message("macro.progress.title"),
      project,
      null,
      MessageHelper.message("macro.progress.stop"),
    )

    if (potemkinProgress == null) potemkinProgress = myPotemkinProgress
    myPotemkinProgress.isIndeterminate = false
    myPotemkinProgress.fraction = 0.0
    try {
      myPotemkinProgress.text2 = if (isInternalMacro) "Executing internal macro" else ""
      val runnable = runnable@{
        try {
          // Handle one keystroke then queue up the next key
          for (i in 0 until total) {
            try {
              myPotemkinProgress.fraction = (i + 1).toDouble() / total
              while (keyStack.hasStroke()) {
                val key = keyStack.feedStroke()
                myPotemkinProgress.checkCanceled()
                val keyHandler = getInstance()
                // During the macro execution, we might change the editor. After that, all
                //  After that, the next operations should be applied to the new editor.
                //  Because of that, we don't use the initially taken editor, but we re-request it on each
                //  macro "step".
                val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor?.vim
                if (currentEditor != null) {
                  ProgressManager.getInstance().executeNonCancelableSection {
                    // Prevent autocompletion during macros.
                    // See https://github.com/JetBrains/ideavim/pull/772 for details
                    CompletionServiceImpl.setCompletionPhase(CompletionPhase.NoCompletion)
                    keyHandler.handleKey(currentEditor, key, context, keyHandler.keyHandlerState)
                  }
                }
                if (injector.messages.isError()) return@runnable
              }
            } finally {
              keyStack.resetFirst()
            }
          }
        } finally {
          keyStack.removeFirst()
        }
      }

      if (isInternalMacro) {
        runnable()
      } else {
        myPotemkinProgress.runInSwingThread(runnable)
      }
    } finally {
      potemkinProgress = null
    }
  }

  companion object {
    private val logger = logger<MacroGroup>()
  }
}
