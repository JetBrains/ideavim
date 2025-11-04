/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.internal

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

internal class HelloWorldAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val notification = Notification(
      "ideavim-sticky",
      "IdeaVim",
      "Hello World!",
      NotificationType.INFORMATION
    )
    Notifications.Bus.notify(notification, e.project)
  }
}
