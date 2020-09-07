/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.actions.RevealFileAction
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.listener.FindActionId
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.KeyStroke
import javax.swing.event.HyperlinkEvent

/**
 * @author Alex Plate
 *
 * This service is can be used as application level and as project level service.
 * If project is null, this means that this is an application level service and notification will be shown for all projects
 */
class NotificationService(private val project: Project?) {
  // This constructor is used to create an applicationService
  @Suppress("unused")
  constructor() : this(null)

  fun notifyAboutIdeaPut() {
    val notification = Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
      """Add <code>ideaput</code> to <code>clipboard</code> option to perform a put via the IDE<br/><b><code>set clipboard+=ideaput</code></b>""",
      NotificationType.INFORMATION)

    notification.addAction(OpenIdeaVimRcAction(notification))

    notification.addAction(AppendToIdeaVimRcAction(notification, "set clipboard+=ideaput", "ideaput") { OptionsManager.clipboard.append(ClipboardOptionsData.ideaput) })

    notification.notify(project)
  }

  fun notifyAboutIdeaJoin() {
    val notification = Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
      """Put <b><code>set ideajoin</code></b> into your <code>~/.ideavimrc</code> to perform a join via the IDE""",
      NotificationType.INFORMATION)

    notification.addAction(OpenIdeaVimRcAction(notification))

    notification.addAction(AppendToIdeaVimRcAction(notification, "set ideajoin", "ideajoin") { OptionsManager.ideajoin.set() })

    notification.addAction(HelpLink(notification, ideajoinExamplesUrl))
    notification.notify(project)
  }

  fun enableRepeatingMode() = Messages.showYesNoDialog("Do you want to enable repeating keys in Mac OS X on press and hold?\n\n" +
    "(You can do it manually by running 'defaults write -g " +
    "ApplePressAndHoldEnabled 0' in the console).", IDEAVIM_NOTIFICATION_TITLE,
    Messages.getQuestionIcon())

  fun specialKeymap(keymap: Keymap, listener: NotificationListener.Adapter) {
    IDEAVIM_STICKY_GROUP.createNotification(IDEAVIM_NOTIFICATION_TITLE,
      "IdeaVim plugin doesn't use the special \"Vim\" keymap any longer. " +
        "Switching to \"${keymap.presentableName}\" keymap.<br/><br/>" +
        "Now it is possible to set up:<br/>" +
        "<ul>" +
        "<li>Vim keys in your ~/.ideavimrc file using key mapping commands</li>" +
        "<li>IDE action shortcuts in \"File | Settings | Keymap\"</li>" +
        "<li>Vim or IDE handlers for conflicting shortcuts in <a href='#settings'>Vim Emulation</a> settings</li>" +
        "</ul>", NotificationType.INFORMATION, listener).notify(project)
  }

  fun noVimrcAsDefault() = IDEAVIM_STICKY_GROUP.createNotification(IDEAVIM_NOTIFICATION_TITLE, "",
    "The ~/.vimrc file is no longer read by default, use ~/.ideavimrc instead. You can read it from your " +
      "~/.ideavimrc using this command:<br/><br/>" +
      "<code>source ~/.vimrc</code>", NotificationType.INFORMATION).notify(project)

  fun notifyAboutShortcutConflict(keyStroke: KeyStroke) {
    VimPlugin.getKey().savedShortcutConflicts[keyStroke] = ShortcutOwner.VIM
    val shortcutText = KeymapUtil.getShortcutText(KeyboardShortcut(keyStroke, null))
    val message = "Using the <b>$shortcutText</b> shortcut for Vim emulation.<br/>" +
      "You can redefine it as an <a href='#ide'>IDE shortcut</a> or " +
      "configure its handler in <a href='#settings'>Vim Emulation</a> settings."
    val listener = object : NotificationListener.Adapter() {
      override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
        when (e.description) {
          "#ide" -> {
            VimPlugin.getKey().savedShortcutConflicts[keyStroke] = ShortcutOwner.IDE
            notification.expire()
          }
          "#settings" -> ShowSettingsUtil.getInstance().editConfigurable(project, VimEmulationConfigurable())
        }
      }
    }
    Notification(IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      message,
      NotificationType.INFORMATION,
      listener).notify(project)
  }

  fun notifySubscribedToEap() {
    Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
      """You are successfully subscribed to IdeaVim EAP releases.""",
      NotificationType.INFORMATION).notify(project)
  }

  fun notifyEapFinished() {
    Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
      """You have finished the Early Access Program. Please reinstall IdeaVim to get the stable version.""",
      NotificationType.INFORMATION).notify(project)
  }

  fun notifyActionId(id: String?) {
    ActionIdNotifier.notifyActionId(id, project)
  }

  private object ActionIdNotifier {
    private var notification: Notification? = null
    private const val NO_ID = "<i>No Action Id</i>"

    fun notifyActionId(id: String?, project: Project?) {

      notification?.expire()

      Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, "Action id: ${id ?: NO_ID}", NotificationType.INFORMATION).let {
        notification = it
        it.whenExpired { notification = null }
        it.setContent(it.content + "<br><br><small>Use Event Log to see previous ids</small>")

        val copyActionId = CopyActionId(id, project)
        copyActionId.templatePresentation.isEnabled = id != null
        it.addAction(copyActionId)

        it.addAction(StopTracking())
        it.notify(project)
      }
    }

    class CopyActionId(val id: String?, val project: Project?) : DumbAwareAction("Copy Action Id") {
      override fun actionPerformed(e: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(StringSelection(id ?: ""))
        notification?.expire()

        val content = if (id == null) "No action id" else "Action id copied: $id"
        Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, content, NotificationType.INFORMATION).let {
          it.addAction(StopTracking())
          it.notify(project)
        }
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = id != null
      }
    }

    class StopTracking : DumbAwareAction("Stop Tracking") {
      override fun actionPerformed(e: AnActionEvent) {
        FindActionId.enabled = false
        notification?.expire()
      }
    }
  }

  class OpenIdeaVimRcAction(private val notification: Notification?) : DumbAwareAction("Open ~/.ideavimrc") {
    override fun actionPerformed(e: AnActionEvent) {
      val eventProject = e.project
      if (eventProject != null) {
        val ideaVimRc = VimScriptParser.findOrCreateIdeaVimRc()
        if (ideaVimRc != null) {
          OpenFileAction.openFile(ideaVimRc.path, eventProject)
          // Do not expire a notification. The user should see what they are entering
          return
        }
      }
      notification?.expire()
      createIdeaVimRcManually("Cannot create configuration file.<br/>Please create <code>~/.ideavimrc</code> manually", eventProject)
    }
  }

  private inner class AppendToIdeaVimRcAction(val notification: Notification, val appendableText: String, val optionName: String, val enableOption: () -> Unit) : AnAction("Append to ~/.ideavimrc") {
    override fun actionPerformed(e: AnActionEvent) {
      val eventProject = e.project
      enableOption()
      if (eventProject != null) {
        val ideaVimRc = VimScriptParser.findOrCreateIdeaVimRc()
        if (ideaVimRc != null && ideaVimRc.canWrite()) {
          ideaVimRc.appendText(appendableText)
          notification.expire()
          val successNotification = Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, "<code>$optionName</code> is enabled", NotificationType.INFORMATION)
          successNotification.addAction(OpenIdeaVimRcAction(successNotification))
          successNotification.notify(project)
          return
        }
      }
      notification.expire()
      createIdeaVimRcManually("Option is enabled, but the file is not modified<br/>Please modify <code>~/.ideavimrc</code> manually", project)
    }
  }

  private inner class HelpLink(val notification: Notification, val link: String) : AnAction("", "", AllIcons.General.TodoQuestion) {
    override fun actionPerformed(e: AnActionEvent) {
      BrowserLauncher.instance.open(link)
      notification.expire()
    }
  }

  companion object {
    val IDEAVIM_STICKY_GROUP = NotificationGroup("ideavim-sticky", NotificationDisplayType.STICKY_BALLOON, false)
    const val IDEAVIM_NOTIFICATION_ID = "ideavim"
    const val IDEAVIM_NOTIFICATION_TITLE = "IdeaVim"
    const val ideajoinExamplesUrl = "https://github.com/JetBrains/ideavim/wiki/%60ideajoin%60-examples"

    private fun createIdeaVimRcManually(message: String, project: Project?) {
      val notification = Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, message, NotificationType.WARNING)
      var actionName = if (SystemInfo.isMac) "Reveal Home in Finder" else "Show Home in " + RevealFileAction.getFileManagerName()
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
