/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.troubleshooting

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.icons.VimIcons
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import java.util.function.Function
import javax.swing.JComponent

private var warningExplicitlyDisabled = false

private object CommandsCounter {
  var commandsBeforeAutoDisable = 10
  var initialized = false

  @Synchronized
  fun init() {
    if (initialized) return
    initialized = true
    KeyHandler.getInstance().addCommandListener {
      commandsBeforeAutoDisable -= 1
      if (commandsBeforeAutoDisable <= 0) {
        warningExplicitlyDisabled = true
      }
    }
  }
}

/**
 * Warning for the new users who may install IdeaVim plugin accidentally.
 */
internal class AccidentalInstallDetectorEditorNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(
    project: Project,
    file: VirtualFile,
  ): Function<in FileEditor, out JComponent?>? {

    CommandsCounter.init()

    // Note: Currently, enable this only for GoLand as it was a request from this IDE (VIM-3784).
    //   However, we can enable it for other IDEs if needed.
    if (!PlatformUtils.isGoIde()) return null

    if (warningExplicitlyDisabled) return null
    if (VimPlugin.isNotEnabled()) return null
    if (VimRcService.findIdeaVimRc() != null) return null
    if (!injector.enabler.isNewIdeaVimUser()) return null

    return Function { fileEditor: FileEditor ->
      val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info)
      panel.text = getText()
      panel.icon(VimIcons.IDEAVIM)

      KeyHandler.getInstance().addCommandListener {
        if (CommandsCounter.commandsBeforeAutoDisable <= 0) {
          KeyHandler.getInstance().removeAllCommandListeners()
          EditorNotifications.getInstance(project).removeNotificationsForProvider(this)
        }
        panel.text = getText()
        panel.invalidate()
        panel.repaint()
      }

      @Suppress("DialogTitleCapitalization")
      panel.createActionLabel("Disable IdeaVim") {
        VimPlugin.setEnabled(false)
        VimPlugin.getNotifications(project).showReenableNotification(project)
        EditorNotifications.getInstance(project).removeNotificationsForProvider(this)
        warningExplicitlyDisabled = true
      }
      panel.createActionLabel("Dismiss") {
        EditorNotifications.getInstance(project).removeNotificationsForProvider(this)
        warningExplicitlyDisabled = true
      }
      panel
    }
  }

  private fun getText(): String {
    return "<html>You’re using the IdeaVim plugin. If you’re not familiar with Vim, consider disabling it. This message will disappear after ${CommandsCounter.commandsBeforeAutoDisable} commands.</html>"
  }
}
