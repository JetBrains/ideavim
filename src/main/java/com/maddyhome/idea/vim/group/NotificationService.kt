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
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.ex.KeymapManagerEx
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.handler.KeyMapIssue
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.key.ShortcutOwnerInfo
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.ijOptions
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.statistic.ActionTracker
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable
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
@Service(Service.Level.PROJECT, Service.Level.APP)
internal class NotificationService(private val project: Project?) {
  // This constructor is used to create an applicationService
  @Suppress("unused")
  constructor() : this(null)

  fun notifyAboutNewUndo() {
    val notification = Notification(
      IDEAVIM_NOTIFICATION_ID,
      "Undo in IdeaVim now works like in Vim",
      """
        Caret movement is no longer a separate undo step, and full insert is undoable in one step.
      """.trimIndent(),
      NotificationType.INFORMATION,
    )

    notification.addAction(object : DumbAwareAction("Share Feedback") {
      override fun actionPerformed(p0: AnActionEvent) {
        BrowserUtil.browse("https://youtrack.jetbrains.com/issue/VIM-547/Undo-splits-Insert-mode-edits-into-separate-undo-chunks")
      }
    })

    notification.notify(project)
  }

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

  fun notifyAboutIdeaJoin(editor: VimEditor) {
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
        "ideajoin"
      ) {
        // This is a global-local option. Setting it will always set the global value
        injector.ijOptions(editor).ideajoin = true
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

  fun notifyActionId(id: String?, candidates: List<String>? = null) {
    ActionIdNotifier.notifyActionId(id, project, candidates)
  }

  fun notifyKeymapIssues(issues: ArrayList<KeyMapIssue>) {
    val keymapManager = KeymapManagerEx.getInstanceEx()
    val keymap = keymapManager.activeKeymap
    val message = buildString {
      appendLine("Current IDE keymap (${keymap.name}) has issues:<br/>")
      issues.forEach {
        when (it) {
          is KeyMapIssue.AddShortcut -> {
            appendLine("- ${it.key} key is not assigned to the ${it.action} action.<br/>")
          }

          is KeyMapIssue.RemoveShortcut -> {
            appendLine("- ${it.shortcut} key is incorrectly assigned to the ${it.action} action.<br/>")
          }
        }
      }
    }
    val notification = IDEAVIM_STICKY_GROUP.createNotification(
      IDEAVIM_NOTIFICATION_TITLE,
      message,
      NotificationType.ERROR,
    )
    notification.subtitle = "IDE keymap misconfigured"
    notification.addAction(object : DumbAwareAction("Fix Keymap") {
      override fun actionPerformed(e: AnActionEvent) {
        issues.forEach {
          when (it) {
            is KeyMapIssue.AddShortcut -> {
              keymap.addShortcut(it.actionId, KeyboardShortcut(it.keyStroke, null))
            }

            is KeyMapIssue.RemoveShortcut -> {
              keymap.removeShortcut(it.actionId, it.shortcut)
            }
          }
        }
        LOG.info("Shortcuts updated $issues")
        notification.expire()
        requiredShortcutsAssigned()
      }
    })
    notification.addAction(object : DumbAwareAction("Open Keymap Settings") {
      override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.project, KeymapPanel::class.java)
        notification.hideBalloon()
      }
    })
    notification.addAction(object : DumbAwareAction("Ignore") {
      override fun actionPerformed(e: AnActionEvent) {
        LOG.info("Ignored to update shortcuts $issues")
        notification.hideBalloon()
      }
    })
    notification.notify(project)
  }

  private fun requiredShortcutsAssigned() {
    val notification = Notification(
      IDEAVIM_NOTIFICATION_ID,
      IDEAVIM_NOTIFICATION_TITLE,
      "Keymap fixed",
      NotificationType.INFORMATION,
    )
    notification.addAction(object : DumbAwareAction("Open Keymap Settings") {
      override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.project, KeymapPanel::class.java)
        notification.hideBalloon()
      }
    })
    notification.notify(project)
  }

  object ActionIdNotifier {
    private var notification: Notification? = null

    fun notifyActionId(id: String?, project: Project?, candidates: List<String>? = null) {
      notification?.expire()

      val possibleIDs = candidates?.distinct()?.sorted()
      val content = when {
        id != null -> "Action ID: <code>$id</code><br><br>"
        possibleIDs.isNullOrEmpty() -> "<i>Cannot detect action ID</i><br><br>"
        possibleIDs.size == 1 -> "Possible action ID: <code>${possibleIDs[0]}</code><br><br>"
        else -> {
          buildString {
            append("<p>Multiple possible action IDs. Candidates include:<ul>")
            possibleIDs.forEach { append("<li><code>$it</code></li>") }
            append("</ul></p>")
          }
        }
      } + "<small>See the ${ActionCenter.getToolwindowName()} tool window for previous IDs</small>"

      notification =
        Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, content, NotificationType.INFORMATION).also {
          it.whenExpired { notification = null }
          it.addAction(StopTracking())

          if (id != null || possibleIDs?.size == 1) {
            it.addAction(CopyActionId(id ?: possibleIDs?.get(0), project))
          }

          it.notify(project)
        }

      if (id != null) {
        ActionTracker.Util.logTrackedAction(id)
      }
    }

    class CopyActionId(val id: String?, val project: Project?) :
      DumbAwareAction(MessageHelper.message("action.copy.action.id.text")) {
      override fun actionPerformed(e: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(StringSelection(id ?: ""))
        if (id != null) {
          ActionTracker.Util.logCopiedAction(id)
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

    private val LOG = logger<NotificationService>()

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
