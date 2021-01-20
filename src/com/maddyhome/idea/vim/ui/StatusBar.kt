/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.option.IdeaStatusIcon
import com.maddyhome.idea.vim.option.OptionsManager
import icons.VimIcons
import org.jetbrains.annotations.NonNls
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.SwingConstants

@NonNls
const val STATUS_BAR_ICON_ID = "IdeaVim-Icon"
const val STATUS_BAR_DISPLAY_NAME = "IdeaVim"

class StatusBarIconFactory : StatusBarWidgetFactory/*, LightEditCompatible*/ {

  override fun getId(): String = STATUS_BAR_ICON_ID

  override fun getDisplayName(): String = STATUS_BAR_DISPLAY_NAME

  override fun disposeWidget(widget: StatusBarWidget) {
    // Nothing
  }

  override fun isAvailable(project: Project): Boolean {
    return OptionsManager.ideastatusicon.value != IdeaStatusIcon.disabled
  }

  override fun createWidget(project: Project): StatusBarWidget {
    OptionsManager.ideastatusicon.addOptionChangeListener { _, _ -> updateAll() }
    return VimStatusBar()
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  /* Use can configure this icon using ideastatusicon option, but we should still keep the option to remove
  * the icon via IJ because this option is hard to discover */
  override fun isConfigurable(): Boolean = true

  private fun updateAll() {
    val projectManager = ProjectManager.getInstanceIfCreated() ?: return
    for (project in projectManager.openProjects) {
      val statusBarWidgetsManager = project.getService(StatusBarWidgetsManager::class.java) ?: continue
      statusBarWidgetsManager.updateWidget(this)
    }

    updateIcon()
  }

  companion object {
    fun updateIcon() {
      val projectManager = ProjectManager.getInstanceIfCreated() ?: return
      for (project in projectManager.openProjects) {
        val statusBar = WindowManager.getInstance().getStatusBar(project) ?: continue
        statusBar.updateWidget(STATUS_BAR_ICON_ID)
      }
    }
  }
}

class VimStatusBar : StatusBarWidget, StatusBarWidget.IconPresentation {

  override fun ID(): String = STATUS_BAR_ICON_ID

  override fun install(statusBar: StatusBar) {
    // Nothing
  }

  override fun dispose() {
    // Nothing
  }

  override fun getTooltipText() = STATUS_BAR_DISPLAY_NAME

  override fun getIcon(): Icon {
    if (OptionsManager.ideastatusicon.value == IdeaStatusIcon.gray) return VimIcons.IDEAVIM_DISABLED
    return if (VimPlugin.isEnabled()) VimIcons.IDEAVIM else VimIcons.IDEAVIM_DISABLED
  }

  override fun getClickConsumer() = Consumer<MouseEvent> { event ->
    val component = event.component
    val popup = VimActionsPopup.getPopup(DataManager.getInstance().getDataContext(component))
    val dimension = popup.content.preferredSize

    val at = Point(0, -dimension.height)
    popup.show(RelativePoint(component, at))
  }

  override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
}

class VimActions : DumbAwareAction() {

  companion object {
    const val actionPlace = "VimActionsPopup"
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    VimActionsPopup.getPopup(e.dataContext).showCenteredInCurrentWindow(project)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && !project.isDisposed
  }
}

private object VimActionsPopup {
  fun getPopup(dataContext: DataContext): ListPopup {
    val actions = getActions()
    val popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(
        STATUS_BAR_DISPLAY_NAME, actions,
        dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false,
        VimActions.actionPlace
      )
    popup.setAdText(MessageHelper.message("popup.advertisement.version", VimPlugin.getVersion()), SwingConstants.CENTER)

    return popup
  }

  private fun getActions(): DefaultActionGroup {
    val actionGroup = DefaultActionGroup()
    actionGroup.isPopup = true

    actionGroup.add(ActionManager.getInstance().getAction("VimPluginToggle"))
    actionGroup.addSeparator()
    actionGroup.add(NotificationService.OpenIdeaVimRcAction(null))
    actionGroup.add(ShortcutConflictsSettings)
    actionGroup.addSeparator()

    val eapGroup = DefaultActionGroup(
      MessageHelper.message("action.eap.choice.active.text", if (JoinEap.eapActive()) 0 else 1),
      true
    )
    eapGroup.add(JoinEap)
    eapGroup.add(
      HelpLink(
        MessageHelper.message("action.about.eap.text"),
        "https://github.com/JetBrains/ideavim#get-early-access",
        null
      )
    )
    actionGroup.add(eapGroup)

    val helpGroup = DefaultActionGroup(MessageHelper.message("action.contacts.help.text"), true)
    helpGroup.add(
      HelpLink(
        MessageHelper.message("action.contact.on.twitter.text"),
        "https://twitter.com/ideavim",
        VimIcons.TWITTER
      )
    )
    helpGroup.add(
      HelpLink(
        MessageHelper.message("action.create.issue.text"),
        "https://youtrack.jetbrains.com/issues/VIM",
        VimIcons.YOUTRACK
      )
    )
    helpGroup.add(
      HelpLink(
        MessageHelper.message("action.contribute.on.github.text"),
        "https://github.com/JetBrains/ideavim",
        VimIcons.GITHUB
      )
    )
    actionGroup.add(helpGroup)

    return actionGroup
  }
}

private class HelpLink(
  // [VERSION UPDATE] 203+ uncomment
  /*@ActionText*/ name: String,
                  val link: String,
                  icon: Icon?
) : DumbAwareAction(name, null, icon)/*, LightEditCompatible*/ {
  override fun actionPerformed(e: AnActionEvent) {
    BrowserUtil.browse(link)
  }
}

private object ShortcutConflictsSettings : DumbAwareAction(MessageHelper.message("action.settings.text"))/*, LightEditCompatible*/ {
  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().editConfigurable(e.project, VimEmulationConfigurable())
  }
}

private object JoinEap : DumbAwareAction()/*, LightEditCompatible*/ {
  private const val EAP_LINK = "https://plugins.jetbrains.com/plugins/eap/ideavim"

  fun eapActive() = EAP_LINK in UpdateSettings.getInstance().storedPluginHosts

  override fun actionPerformed(e: AnActionEvent) {
    if (eapActive()) {
      UpdateSettings.getInstance().storedPluginHosts -= EAP_LINK
      VimPlugin.getNotifications(e.project).notifyEapFinished()
    } else {
      UpdateSettings.getInstance().storedPluginHosts += EAP_LINK
      VimPlugin.getNotifications(e.project).notifySubscribedToEap()
    }
  }

  override fun update(e: AnActionEvent) {
    if (eapActive()) {
      e.presentation.text = MessageHelper.message("action.finish.eap.text")
    } else {
      e.presentation.text = MessageHelper.message("action.subscribe.to.eap.text")
    }
  }
}
