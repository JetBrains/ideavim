/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.psi.PsiManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.ui.LafIconLookup
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.group.IjOptions
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.icons.VimIcons
import com.maddyhome.idea.vim.newapi.globalIjOptions
import org.jetbrains.annotations.NonNls
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.SwingConstants
import javax.swing.Timer

@NonNls
internal const val STATUS_BAR_ICON_ID = "IdeaVim-Icon"
internal const val STATUS_BAR_DISPLAY_NAME = "IdeaVim"

internal class StatusBarIconFactory : StatusBarWidgetFactory/*, LightEditCompatible*/ {

  override fun getId(): String = STATUS_BAR_ICON_ID

  override fun getDisplayName(): String = STATUS_BAR_DISPLAY_NAME

  override fun disposeWidget(widget: StatusBarWidget) {
    // Nothing
  }

  override fun isAvailable(project: Project): Boolean {
    return injector.globalIjOptions().ideastatusicon != IjOptionConstants.ideastatusicon_disabled
  }

  override fun createWidget(project: Project): StatusBarWidget {
    VimPlugin.getOptionGroup().addGlobalOptionChangeListener(IjOptions.ideastatusicon) { updateAll() }

    // Double update the status bar icon with 5-second delay
    // There is an issue VIM-3084 that must probably caused by some race between status bar icon initialization
    //   and .ideavimrc reading. I believe this is a simple fix for it.
    val timer = Timer(5_000) { updateAll() }
    timer.isRepeats = false
    timer.start()

    return VimStatusBar()
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  /* Use can configure this icon using ideastatusicon option, but we should still keep the option to remove
  * the icon via IJ because this option is hard to discover */
  override fun isConfigurable(): Boolean = true

  @Suppress("IncorrectServiceRetrieving")
  private fun updateAll() {
    val projectManager = ProjectManager.getInstanceIfCreated() ?: return
    for (project in projectManager.openProjects) {
      // The StatusBarWidgetsManager IS a project-level service.
      val statusBarWidgetsManager = project.getService(StatusBarWidgetsManager::class.java) ?: continue
      statusBarWidgetsManager.updateWidget(this)
    }

    Util.updateIcon()
  }

  object Util {
    fun updateIcon() {
      val projectManager = ProjectManager.getInstanceIfCreated() ?: return
      for (project in projectManager.openProjects) {
        val statusBar = WindowManager.getInstance().getStatusBar(project) ?: continue
        statusBar.updateWidget(STATUS_BAR_ICON_ID)
      }
    }
  }
}

internal class VimStatusBar : StatusBarWidget, StatusBarWidget.IconPresentation {

  override fun ID(): String = STATUS_BAR_ICON_ID

  override fun install(statusBar: StatusBar) {
    // Nothing
  }

  override fun dispose() {
    // Nothing
  }

  override fun getTooltipText() = STATUS_BAR_DISPLAY_NAME

  override fun getIcon(): Icon {
    if (injector.globalIjOptions().ideastatusicon == IjOptionConstants.ideastatusicon_gray) {
      return VimIcons.IDEAVIM_DISABLED
    }
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

internal class VimActions : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    VimActionsPopup.getPopup(e.dataContext).showCenteredInCurrentWindow(project)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && !project.isDisposed
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private object VimActionsPopup {
  fun getPopup(dataContext: DataContext): ListPopup {
    val actions = getActions()
    val popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(
        STATUS_BAR_DISPLAY_NAME,
        actions,
        dataContext,
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        false,
        ActionPlaces.POPUP,
      )
    popup.setAdText(MessageHelper.message("widget.vim.actions.popup.advertisement.version", VimPlugin.getVersion()), SwingConstants.CENTER)

    return popup
  }

  private fun getActions(): DefaultActionGroup {
    val actionGroup = DefaultActionGroup()
    actionGroup.isPopup = true

    actionGroup.add(ActionManager.getInstance().getAction("VimPluginToggle"))
    actionGroup.addSeparator()
    actionGroup.add(NotificationService.OpenIdeaVimRcAction(null))
    actionGroup.add(ShortcutConflictsSettings)
    actionGroup.add(
      HelpLink(
        "Plugin ↗",
        "https://jb.gg/ideavim-plugins",
        null,
      ),
    )
    actionGroup.add(
      HelpLink(
        "Take Survey ↗",
        "https://jb.gg/axootm",
        AllIcons.Actions.IntentionBulb,
      ),
    )
    actionGroup.addSeparator(MessageHelper.message("widget.vim.actions.popup.eap.choice.active.text"))

    actionGroup.add(JoinEap)
    actionGroup.add(
      HelpLink(
        "About EAP ↗",
        "https://github.com/JetBrains/ideavim#get-early-access",
        null,
      ),
    )

    actionGroup.addSeparator("Learn")

    actionGroup.add(TutorAction())
    actionGroup.add(
      HelpLink(
        "IdeaVim Docs ↗",
        "https://github.com/JetBrains/ideavim?tab=readme-ov-file#summary-of-supported-vim-features",
        null,
      ),
    )

    actionGroup.addSeparator(MessageHelper.message("widget.vim.actions.popup.contacts.help.text"))
    actionGroup.add(
      HelpLink(
        MessageHelper.message("widget.vim.actions.popup.contact.on.twitter.text"),
        "https://twitter.com/ideavim",
        VimIcons.TWITTER,
      ),
    )
    actionGroup.add(
      HelpLink(
        MessageHelper.message("widget.vim.actions.popup.create.issue.text"),
        "https://youtrack.jetbrains.com/newIssue?project=VIM&description=%0A%0A-----------%0AYou%20can%20improve%20the%20issue%20description%20by%20providing%3A%0A1)%20Your%20%60~%2F.ideavimrc%60%20configuration%20if%20you%20use%20it.%0A2)%20The%20%5Blog%5D(https%3A%2F%2Fintellij-support.jetbrains.com%2Fhc%2Fen-us%2Farticles%2F207241085-Locating-IDE-log-files)%20from%20your%20IDE.%0A%0AVersion:%20${VimPlugin.getVersion()}&c=Affected%20versions%20${VimPlugin.getVersion()}",
        VimIcons.YOUTRACK,
      ),
    )
    actionGroup.add(
      HelpLink(
        MessageHelper.message("widget.vim.actions.popup.contribute.on.github.text"),
        "https://github.com/JetBrains/ideavim",
        AllIcons.Vcs.Vendors.Github,
      ),
    )

    return actionGroup
  }
}

private class TutorAction : DumbAwareAction("Tutor") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val file = ScratchRootType.getInstance()
      .createScratchFile(project, "Tutor.txt", PlainTextLanguage.INSTANCE, tutor) ?: return

    PsiNavigationSupport.getInstance()
      .createNavigatable(project, file, 0)
      .navigate(!LaterInvocator.isInModalContextForProject(project))

    PsiManager.getInstance(project).findFile(file)?.let {
      ProjectView.getInstance(project).selectPsiElement(it, false)
    }
  }
}

private class HelpLink(
  @NlsActions.ActionText name: String,
  val link: String,
  icon: Icon?,
) : DumbAwareAction(name, null, icon)/*, LightEditCompatible*/ {
  override fun actionPerformed(e: AnActionEvent) {
    BrowserUtil.browse(link)
  }
}

private object ShortcutConflictsSettings :
  DumbAwareAction(MessageHelper.message("widget.vim.actions.popup.settings.text"))/*, LightEditCompatible*/ {
  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().showSettingsDialog(e.project, VimEmulationConfigurable::class.java)
  }
}

internal object JoinEap : DumbAwareAction()/*, LightEditCompatible*/ {
  const val EAP_LINK = "https://plugins.jetbrains.com/plugins/eap/ideavim"

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
      e.presentation.icon = LafIconLookup.getIcon("checkmark")
      e.presentation.text = MessageHelper.message("widget.vim.actions.popup.finish.eap.text")
    } else {
      e.presentation.text = MessageHelper.message("widget.vim.actions.popup.subscribe.to.eap.text")
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
