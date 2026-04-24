/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.api.VimEditor
import javax.swing.KeyStroke

/**
 * Interface for notification service methods accessible from the common module.
 * The full [NotificationService] implementation lives in the frontend module.
 */
interface VimNotifications {
  companion object {
    const val IDEAVIM_NOTIFICATION_ID = "ideavim"
    const val IDEAVIM_NOTIFICATION_TITLE = "IdeaVim"
  }

  fun notifyAboutIdeaPut()
  fun notifyAboutIdeaJoin(editor: VimEditor)
  fun enableRepeatingMode(): Int
  fun noVimrcAsDefault()
  fun notifyAboutShortcutConflict(keyStroke: KeyStroke)
  fun notifySubscribedToEap()
  fun notifyEapFinished()
  fun showReenableNotification(project: Project)
  fun notifyActionId(id: String?, candidates: List<String>? = null, intentionName: String?)
}
