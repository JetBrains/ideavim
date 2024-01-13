/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.util.*

public interface VimStatusBarWidget {
  public fun updateWidgetInStatusBar(widgetID: String, project: Project?) {
    if (project == null) return
    val updateWidgetTask = TimerWithRetriesTask(500L, 50) {
      val windowManager = WindowManager.getInstance()
      val statusBar = windowManager.getStatusBar(project) ?: return@TimerWithRetriesTask
      statusBar.updateWidget(widgetID)
    }
    updateWidgetTask.execute()
  }
}

/**
 * A task that may be used to address issues with initialization in the Platform, executing code with a reasonable number of retries and a reasonable period.
 * Clearly, this is a workaround and its use should be avoided when possible.
 *
 * Why is it needed for widgets?
 * In a single project environment, it is not necessary since the status bar is initialized before the editor opens.
 * However, in multi-project setups, the editor window is opened before the status bar initialization.
 * And this tasks tries to loops until status bar creation in order to notify it about opened editor.
 */
private class TimerWithRetriesTask(
  private val period: Long,
  private val retriesLimit: Int,
  private val block: () -> Unit
) {
  private val timer = Timer()

  fun execute() {
    timer.schedule(object : TimerTask() {
      private var counter = 0

      override fun run() {
        if (counter >= retriesLimit) {
          timer.cancel()
        } else {
          this@TimerWithRetriesTask.block()
          counter++
        }
      }
    }, 0L, period)
  }
}
