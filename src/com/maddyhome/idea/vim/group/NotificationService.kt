package com.maddyhome.idea.vim.group

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.OpenFileAction
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.TextWithMnemonic
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser
import com.maddyhome.idea.vim.key.ShortcutOwner
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable
import java.io.File
import javax.swing.KeyStroke
import javax.swing.event.HyperlinkEvent

/**
 * @author Alex Plate
 */
class NotificationService(private val project: Project?) {
  // This constructor is used to create an applicationService
  @Suppress("unused")
  constructor() : this(null)

  fun notifyAboutIdeaJoin() {
    val notification = Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
      """Put <b><code>set ideajoin</code></b> into your <code>~/.ideavimrc</code> to perform a join via the IDE""",
      NotificationType.INFORMATION)

    notification.addAction(object : AnAction() {
      init {
        this.templatePresentation.setTextWithMnemonic(TextWithMnemonic.parse("_Open ~/.ideavimrc"))
      }

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
        notification.expire()
        createIdeaVimRcManually("Cannot create configuration file.<br/>Please create <code>~/.ideavimrc</code> manually")
      }
    })

    notification.addAction(object : AnAction() {
      init {
        this.templatePresentation.setTextWithMnemonic(TextWithMnemonic.parse("_Append to ~/.ideavimrc"))
      }

      override fun actionPerformed(e: AnActionEvent) {
        val eventProject = e.project
        OptionsManager.ideajoin.set()
        if (eventProject != null) {
          val ideaVimRc = VimScriptParser.findOrCreateIdeaVimRc()
          if (ideaVimRc != null && ideaVimRc.canWrite()) {
            ideaVimRc.appendText("set ideajoin")
            notification.expire()
            Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, "Success.<br/><code>ideajoin</code> is enabled", NotificationType.INFORMATION).notify(project)
            return
          }
        }
        notification.expire()
        createIdeaVimRcManually("Option is enabled, but the file is not modified<br/>Please modify <code>~/.ideavimrc</code> manually")
      }
    })

    notification.addAction(object : AnAction("", "", AllIcons.General.TodoQuestion) {
      override fun actionPerformed(e: AnActionEvent) {
        BrowserLauncher.instance.open(ideajoinExamplesUrl)
        notification.expire()
      }
    })
    notification.notify(project)
  }

  private fun createIdeaVimRcManually(message: String) {
    val notification = Notification(IDEAVIM_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE, message, NotificationType.WARNING)
    var actionName = if (SystemInfo.isMac) "Reveal Home in Finder" else "Show Home in " + ShowFilePathAction.getFileManagerName()
    if (!File(System.getProperty("user.home")).exists()) {
      actionName = ""
    }
    notification.addAction(object : AnAction(actionName) {
      override fun actionPerformed(e: AnActionEvent) {
        val homeDir = File(System.getProperty("user.home"))
        ShowFilePathAction.openDirectory(homeDir)
        notification.expire()
      }
    })
    notification.notify(project)
  }

  fun enableRepeatingMode() = Messages.showYesNoDialog("Do you want to enable repeating keys in Mac OS X on press and hold?\n\n" +
    "(You can do it manually by running 'defaults write -g " +
    "ApplePressAndHoldEnabled 0' in the console).", IDEAVIM_NOTIFICATION_TITLE,
    Messages.getQuestionIcon())

  fun specialKeymap(keymap: Keymap, listener: NotificationListener.Adapter) {
    Notification(IDEAVIM_STICKY_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
      "IdeaVim plugin doesn't use the special \"Vim\" keymap any longer. " +
        "Switching to \"${keymap.presentableName}\" keymap.<br/><br/>" +
        "Now it is possible to set up:<br/>" +
        "<ul>" +
        "<li>Vim keys in your ~/.ideavimrc file using key mapping commands</li>" +
        "<li>IDE action shortcuts in \"File | Settings | Keymap\"</li>" +
        "<li>Vim or IDE handlers for conflicting shortcuts in <a href='#settings'>Vim Emulation</a> settings</li>" +
        "</ul>", NotificationType.INFORMATION, listener).notify(project)
  }

  fun noVimrcAsDefault() = Notification(IDEAVIM_STICKY_NOTIFICATION_ID, IDEAVIM_NOTIFICATION_TITLE,
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

  companion object {
    const val IDEAVIM_STICKY_NOTIFICATION_ID = "ideavim-sticky"
    const val IDEAVIM_NOTIFICATION_ID = "ideavim"
    const val IDEAVIM_NOTIFICATION_TITLE = "IdeaVim"
    const val ideajoinExamplesUrl = "https://github.com/JetBrains/ideavim/blob/master/doc/ideajoin-examples.md"
  }
}