/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import com.maddyhome.idea.vim.ui.ShowCmd
import java.awt.Toolkit

@Service
internal class IjVimMessages : VimMessagesBase() {

  private var message: String? = null
  private var error = false
  private var lastBeepTimeMillis = 0L
  private var allowClearStatusBarMessage = true

  override fun showStatusBarMessage(editor: VimEditor?, message: String?) {
    fun setStatusBarMessage(project: Project, message: String?) {
      WindowManager.getInstance().getStatusBar(project)?.let {
        it.info = if (message.isNullOrBlank()) "" else "Vim - $message"
      }
    }

    this.message = message

    val project = editor?.ij?.project
    if (project != null) {
      setStatusBarMessage(project, message)
    } else {
      // TODO: We really shouldn't set the status bar text for other projects. That's rude.
      ProjectManager.getInstance().openProjects.forEach {
        setStatusBarMessage(it, message)
      }
    }

    // Redraw happens automatically based on changes or scrolling. If we've just set the message (e.g., searching for a
    // string, hitting the bottom and scrolling to the top), make sure we don't immediately clear it when scrolling.
    allowClearStatusBarMessage = false
    ApplicationManager.getApplication().invokeLater {
      allowClearStatusBarMessage = true
    }
  }

  override fun getStatusBarMessage(): String? = message

  // Vim doesn't appear to have a policy about clearing the status bar, other than on "redraw". This can be forced with
  // <C-L> or the `:redraw` command, but also happens as the screen changes, e.g., when inserting or deleting lines,
  // scrolling, entering Command-line mode and probably lots more. We should manually clear the status bar when these
  // things happen.
  override fun clearStatusBarMessage() {
    val currentMessage = message
    if (currentMessage.isNullOrEmpty()) return

    // Don't clear the status bar message if we've only just set it
    if (!allowClearStatusBarMessage) return

    ProjectManager.getInstance().openProjects.forEach { project ->
      WindowManager.getInstance().getStatusBar(project)?.let { statusBar ->
        // Only clear the status bar if it's showing our last message
        if (statusBar.info?.contains(currentMessage) == true) {
          statusBar.info = ""
        }
      }
    }
    message = null
  }

  override fun indicateError() {
    error = true
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      if (!injector.globalOptions().visualbell) {
        // Vim only allows a beep once every half second - :help 'visualbell'
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - lastBeepTimeMillis > 500) {
          Toolkit.getDefaultToolkit().beep()
          lastBeepTimeMillis = currentTimeMillis
        }
      }
    }
  }

  override fun clearError() {
    error = false
  }

  override fun isError(): Boolean = error

  override fun message(key: String, vararg params: Any): String = EngineMessageHelper.message(key, *params)

  override fun updateStatusBar(editor: VimEditor) {
    ShowCmd.update()
  }
}
