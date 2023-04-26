/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.ActionCenter
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.statistic.ActionTracker
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 *
 * This service is can be used as application level and as project level service.
 * If project is null, this means that this is an application level service and notification will be shown for all projects
 */
internal class NotificationService(private val project: Project?) {
  // This constructor is used to create an applicationService
  @Suppress("unused")
  constructor() : this(null)

  fun notifyAboutIdeaPut() {
    val notification = Notification(
      IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      """Add <code>ideaput</code> to <code>clipboard</code> option to perform a put via the IDE<br/><b><code>set clipboard+=ideaput</code></b>""",
      NotificationType.INFORMATION,
    )

    notification.addAction(OpenIdeaVimRcAction(notification))

    notification.addAction(
      AppendToIdeaVimRcAction(
        notification,
        "set clipboard^=ideaput",
        "ideaput",
      ) {
        // Technically, we're supposed to prepend values to clipboard so that it's not added to the "exclude" item.
        // Since we don't handle exclude, it's safe to append. But let's be clean.
        injector.globalOptions().clipboard.prependValue(OptionConstants.clipboard_ideaput)
      },
    )

    notification.notify(project)
  }

  fun notifyAboutIdeaJoin() {
    val notification = Notification(
      IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      """Put <b><code>set ideajoin</code></b> into your <code>~/.ideavimrc</code> to perform a join via the IDE""",
      NotificationType.INFORMATION,
    )

    notification.addAction(OpenIdeaVimRcAction(notification))

    notification.addAction(
      AppendToIdeaVimRcAction(
        notification,
        "set ideajoin",
        "idejoin"
      ) {
        // 'ideajoin' is global-local, so we'll set it as a global value
        injector.optionGroup.setOptionValue(IjOptions.ideajoin, OptionScope.GLOBAL, VimInt.ONE)
      },
    )

    notification.addAction(HelpLink(ideajoinExamplesUrl))
    notification.notify(project)
  }

  fun enableRepeatingMode() = Messages.showYesNoDialog(
    "Do you want to enable repeating keys in macOS on press and hold?\n\n" +
      "(You can do it manually by running 'defaults write -g " +
      "ApplePressAndHoldEnabled 0' in the console).",
    IDEAVIM_NOTIFICATION_TITLE,
    Messages.getQuestionIcon(),
  )

  fun noVimrcAsDefault() {
    val notification = IDEAVIM_STICKY_GROUP.createNotification(
      IDEAVIM_NOTIFICATION_TITLE,
      "The ~/.vimrc file is no longer read by default, use ~/.ideavimrc instead. You can read it from your " +
        "~/.ideavimrc using this command:<br/><br/>" +
        "<code>source ~/.vimrc</code>",
      NotificationType.INFORMATION,
    )
    notification.notify(project)
  }

  fun notifyAboutShortcutConflict(keyStroke: KeyStroke) {
    val conflicts = VimPlugin.getKey().savedShortcutConflicts
    val allValuesAreUndefined =
      conflicts.values.all { it is ShortcutOwnerInfo.PerMode || (it is ShortcutOwnerInfo.AllModes && it.owner == ShortcutOwner.UNDEFINED) }
    val shortcutText = KeymapUtil.getShortcutText(KeyboardShortcut(keyStroke, null))
    val message = if (allValuesAreUndefined) {
      "<b>$shortcutText</b> is defined as a shortcut for both Vim and IntelliJ IDEA. It is now used by Vim, but you can change this."
    } else {
      "<b>$shortcutText</b> is used as a Vim command"
    }

    conflicts[keyStroke] = ShortcutOwnerInfo.allVim
    val notification = Notification(
      IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      message,
      NotificationType.INFORMATION,
    )
    notification.addAction(object : DumbAwareAction("Use as IDE Shortcut") {
      override fun actionPerformed(e: AnActionEvent) {
        conflicts[keyStroke] = ShortcutOwnerInfo.allIde
        notification.expire()
      }
    })
    notification.addAction(object : DumbAwareAction("Configure…") {
      override fun actionPerformed(e: AnActionEvent) {
        notification.expire()
        ShowSettingsUtil.getInstance().showSettingsDialog(project, VimEmulationConfigurable::class.java)
      }
    })
    notification.notify(project)
  }

  fun notifySubscribedToEap() {
    Notification(
      IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      """You are successfully subscribed to IdeaVim EAP releases.""",
      NotificationType.INFORMATION,
    ).notify(project)
  }

  fun notifyEapFinished() {
    Notification(
      IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      """You have finished the Early Access Program. Please reinstall IdeaVim to get the stable version.""",
      NotificationType.INFORMATION,
    ).notify(project)
  }

  fun notifyActionId(id: String?) {
    ActionIdNotifier.notifyActionId(id, project)
  }

  object ActionIdNotifier {
    private var notification: Notification? = null
    private const val NO_ID = "<i>Cannot detect action id</i>"

    fun notifyActionId(id: String?, project: Project?) {
      notification?.expire()

      val content = if (id != null) "Action id: $id" else NO_ID
      Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, content, NotificationType.INFORMATION).let {
        notification = it
        it.whenExpired { notification = null }
        it.setContent(it.content + "<br><br><small>Use ${ActionCenter.getToolwindowName()} to see previous ids</small>")

        it.addAction(StopTracking())

        if (id != null) it.addAction(CopyActionId(id, project))

        it.notify(project)
      }

      if (id != null) {
        ActionTracker.logTrackedAction(id)
      }
    }

    class CopyActionId(val id: String?, val project: Project?) : DumbAwareAction(MessageHelper.message("action.copy.action.id.text")) {
      override fun actionPerformed(e: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(StringSelection(id ?: ""))
        if (id != null) {
          ActionTracker.logCopiedAction(id)
        }
        notification?.expire()

        val content = if (id == null) "No action id" else "Action id copied: $id"
        Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, content, NotificationType.INFORMATION).let {
          notification = it
          it.whenExpired { notification = null }
          it.addAction(StopTracking())
          it.notify(project)
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    class StopTracking : DumbAwareAction("Stop Tracking") {
      override fun actionPerformed(e: AnActionEvent) {
        injector.globalIjOptions().trackactionids = false
        notification?.expire()
      }
    }
  }

  @Suppress("DialogTitleCapitalization")
  class OpenIdeaVimRcAction(private val notification: Notification?) : DumbAwareAction(
    if (VimRcService.findIdeaVimRc() != null) "Open ~/.ideavimrc" else "Create ~/.ideavimrc",
  )/*, LightEditCompatible*/ {
    override fun actionPerformed(e: AnActionEvent) {
      val eventProject = e.project
      if (eventProject != null) {
        val ideaVimRc = VimRcService.findOrCreateIdeaVimRc()
        if (ideaVimRc != null) {
          OpenFileAction.openFile(ideaVimRc.path, eventProject)
          // Do not expire a notification. The user should see what they are entering
          return
        }
      }
      notification?.expire()
      createIdeaVimRcManually(
        "Cannot create configuration file.<br/>Please create <code>~/.ideavimrc</code> manually",
        eventProject,
      )
    }

    override fun update(e: AnActionEvent) {
      super.update(e)
      val actionText = if (VimRcService.findIdeaVimRc() != null) "Open ~/.ideavimrc" else "Create ~/.ideavimrc"
      e.presentation.text = actionText
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  @Suppress("DialogTitleCapitalization")
  private inner class AppendToIdeaVimRcAction(
    val notification: Notification,
    val appendableText: String,
    val optionName: String,
    val enableOption: () -> Unit,
  ) : AnAction("Append to ~/.ideavimrc") {
    override fun actionPerformed(e: AnActionEvent) {
      val eventProject = e.project
      enableOption()
      if (eventProject != null) {
        val ideaVimRc = VimRcService.findOrCreateIdeaVimRc()
        if (ideaVimRc != null && ideaVimRc.canWrite()) {
          ideaVimRc.appendText(appendableText)
          notification.expire()
          val successNotification = Notification(
            IDEAVIM_NOTIFICATION_ID,
            IDEAVIM_NOTIFICATION_TITLE,
            "<code>$optionName</code> is enabled",
            NotificationType.INFORMATION,
          )
          successNotification.addAction(OpenIdeaVimRcAction(successNotification))
          successNotification.notify(project)
          return
        }
      }
      notification.expire()
      createIdeaVimRcManually(
        "Option is enabled, but the file is not modified<br/>Please modify <code>~/.ideavimrc</code> manually",
        project,
      )
    }
  }

  private inner class HelpLink(val link: String) : AnAction("", "", AllIcons.Actions.Help) {
    override fun actionPerformed(e: AnActionEvent) {
      BrowserUtil.browse(link)
    }
  }

  companion object {
    val IDEAVIM_STICKY_GROUP: NotificationGroup =
      NotificationGroupManager.getInstance().getNotificationGroup("ideavim-sticky")
    const val IDEAVIM_NOTIFICATION_ID = "ideavim"
    const val IDEAVIM_NOTIFICATION_TITLE = "IdeaVim"
    const val ideajoinExamplesUrl = "https://jb.gg/f9zji9"

    private fun createIdeaVimRcManually(message: String, project: Project?) {
      val notification =
        Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, message, NotificationType.WARNING)
      var actionName =
        if (SystemInfo.isMac) "Reveal Home in Finder" else "Show Home in " + RevealFileAction.getFileManagerName()
      if (!File(System.getProperty("user.home")).exists()) {
        actionName = ""
      }
      notification.addAction(object : AnAction(actionName) {
        override fun actionPerformed(e: AnActionEvent) {
          val homeDir = File(System.getProperty("user.home"))
          RevealFileAction.openDirectory(homeDir)
          notification.expire()
        }
      })
      notification.notify(project)
    }
  }
}
