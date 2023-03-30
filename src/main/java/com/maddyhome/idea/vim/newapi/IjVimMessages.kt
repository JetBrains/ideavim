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
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.ui.ShowCmd
import java.awt.Toolkit

@Service
internal class IjVimMessages : VimMessagesBase() {

  private var message: String? = null
  private var error = false
  private var lastBeepTimeMillis = 0L

  override fun showStatusBarMessage(editor: VimEditor?, message: String?) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      this.message = message
    }
    val pm = ProjectManager.getInstance()
    val projects = pm.openProjects
    for (project in projects) {
      val bar = WindowManager.getInstance().getStatusBar(project)
      if (bar != null) {
        if (message.isNullOrEmpty()) {
          bar.info = ""
        } else {
          bar.info = "VIM - $message"
        }
      }
    }
  }

  override fun getStatusBarMessage(): String? = message

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

  override fun message(key: String, vararg params: Any): String = MessageHelper.message(key, *params)
  override fun updateStatusBar() {
    ShowCmd.update()
  }
}
