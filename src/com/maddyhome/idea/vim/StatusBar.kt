package com.maddyhome.idea.vim

import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.RepositoryHelper
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.updateSettings.impl.UpdateInstaller
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.intellij.util.Consumer
import com.intellij.util.text.VersionComparatorUtil
import com.maddyhome.idea.vim.action.VimPluginToggleAction
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable
import icons.VimIcons
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.SwingConstants


private class StatusBarIconProvider : StatusBarWidgetProvider {
  override fun getWidget(project: Project) = StatusBarIcon
}

private object StatusBarIcon : StatusBarWidget, StatusBarWidget.IconPresentation {

  private var statusBar: StatusBar? = null

  override fun ID(): String = "IdeaVim-Icon"

  override fun install(statusBar: StatusBar) {
    this.statusBar = statusBar
  }

  override fun dispose() {}

  override fun getTooltipText() = "IdeaVim"

  override fun getIcon(): Icon = if (VimPlugin.isEnabled()) VimIcons.IDEAVIM else VimIcons.IDEAVIM_DISABLED

  override fun getClickConsumer() = Consumer<MouseEvent> { event ->
    val component = event.component
    val actions = getActions(component)
    val popup = JBPopupFactory.getInstance()
      .createActionGroupPopup("IdeaVim", actions,
        DataManager.getInstance().getDataContext(component), false, null,
        actions.childrenCount)
    popup.addListener(object : JBPopupListener {
      override fun beforeShown(event: LightweightWindowEvent) {
        val location = component.locationOnScreen
        val size = popup.size
        popup.setLocation(Point(location.x + component.width - size.width, location.y - size.height))
      }
    })

    popup.setAdText("Version ${VimPlugin.getVersion()}", SwingConstants.CENTER)

    popup.show(component)
  }

  override fun getPresentation(): StatusBarWidget.WidgetPresentation? = this

  private fun getActions(component: Component): DefaultActionGroup {
    val actionGroup = DefaultActionGroup()

    actionGroup.add(VimStatusBarToggle)
    actionGroup.addSeparator("Settings")
    actionGroup.add(NotificationService.OpenIdeaVimRcAction(null))
    actionGroup.add(ShortcutConflictsSettings)
    actionGroup.addSeparator("EAP" + if (JoinEap.eapActive()) " (Active)" else "")
    actionGroup.add(JoinEap)
    actionGroup.add(HelpLink("About EAP", "https://github.com/JetBrains/ideavim#get-early-access", null))
    actionGroup.addSeparator()
    actionGroup.add(Help(component))

    return actionGroup
  }

  fun update() {
    statusBar?.updateWidget(this.ID())
  }
}

private class Help(val component: Component) : AnAction("Contacts && Help") {
  override fun actionPerformed(e: AnActionEvent) {

    val helpGroup = DefaultActionGroup()

    helpGroup.add(HelpLink("Contact on Twitter", "https://twitter.com/ideavim", VimIcons.TWITTER))
    helpGroup.add(HelpLink("Create an Issue", "https://youtrack.jetbrains.com/issues/VIM", VimIcons.YOUTRACK))
    helpGroup.add(HelpLink("Contribute on GitHub", "https://github.com/JetBrains/ideavim", VimIcons.GITHUB))

    val popup = JBPopupFactory.getInstance()
      .createActionGroupPopup("Contacts & Help", helpGroup,
        DataManager.getInstance().getDataContext(component), false, null,
        helpGroup.childrenCount)
    popup.addListener(object : JBPopupListener {
      override fun beforeShown(event: LightweightWindowEvent) {
        val location = component.locationOnScreen
        val size = popup.size
        popup.setLocation(Point(location.x + component.width - size.width, location.y - size.height))
      }
    })

    popup.show(component)
  }

}

class HelpLink(
  name: String,
  val link: String,
  icon: Icon?
) : AnAction(name, null, icon) {
  override fun actionPerformed(e: AnActionEvent) {
    BrowserUtil.browse(link)
  }
}

private object VimStatusBarToggle : VimPluginToggleAction() {
  init {
    this.copyShortcutFrom(ActionManager.getInstance().getAction("VimPluginToggle"))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.text = if (VimPlugin.isEnabled()) "Disable" else "Enable"
  }
}

private object ShortcutConflictsSettings : AnAction("Shortcut Settings") {
  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().editConfigurable(e.project, VimEmulationConfigurable())
  }
}

private object JoinEap : AnAction() {
  private const val EAP_LINK = "https://plugins.jetbrains.com/plugins/eap/ideavim"

  fun eapActive() = EAP_LINK in UpdateSettings.getInstance().storedPluginHosts

  override fun actionPerformed(e: AnActionEvent) {
    if (eapActive()) {
      UpdateSettings.getInstance().storedPluginHosts -= EAP_LINK
      VimPlugin.getNotifications(e.project).notifyEapFinished()
    } else {
      UpdateSettings.getInstance().storedPluginHosts += EAP_LINK
      checkForUpdates(e.project)
    }
  }

  override fun update(e: AnActionEvent) {
    if (eapActive()) {
      e.presentation.text = "Finish EAP"
    } else {
      e.presentation.text = "Get Early Access"
    }
  }

  private fun checkForUpdates(project: Project?) {
    val notificator = VimPlugin.getNotifications(project)
    val pluginDescriptor = PluginManager.getPlugin(VimPlugin.getPluginId()) ?: return

    val state = InstalledPluginsState.getInstance()
    val pluginRef = Ref.create<PluginDownloader>()

    object : Task.Backgroundable(null, "Checking for IdeaVim EAP version", true) {
      override fun run(indicator: ProgressIndicator) {
        val toUpdate = mutableMapOf<PluginId, PluginDownloader>()
        val build = ApplicationInfo.getInstance().build
        for (host in RepositoryHelper.getPluginHosts()) {
          val newPluginDescriptor = RepositoryHelper
            .loadPlugins(host, null, indicator)
            .filter { it.pluginId == VimPlugin.getPluginId() }
            .maxWith(java.util.Comparator { o1, o2 -> VersionComparatorUtil.compare(o1.version, o2.version) })
            ?: continue

          val downloader = PluginDownloader.createDownloader(newPluginDescriptor, host, build)
          state.onDescriptorDownload(pluginDescriptor)
          UpdateChecker.checkAndPrepareToInstall(downloader, state, toUpdate, mutableListOf(), indicator)
        }
        val plugin = toUpdate.values.maxWith(java.util.Comparator { o1, o2 -> VersionComparatorUtil.compare(o1.pluginVersion, o2.pluginVersion) })
        pluginRef.set(plugin)
      }

      override fun onSuccess() {
        val downloader: PluginDownloader = pluginRef.get() ?: run {
          notificator.notifySubscribedToEap()
          return
        }

        val version = downloader.pluginVersion
        @Suppress("MoveVariableDeclarationIntoWhen")
        val res = Messages.showYesNoCancelDialog(null,
          "Do you want to install EAP version of IdeaVim and restart the IDE to apply changes?",
          "IdeaVim $version EAP", "Yes", "Install without restart", "Cancel", null)
        when (res) {
          Messages.YES -> updatePlugin(project, downloader) { updated ->
              if (updated) {
                ApplicationManagerEx.getApplicationEx().restart(true)
              } else {
                notificator.notifyFailedToDownloadEap()
              }
          }
          Messages.NO -> updatePlugin(project, downloader) { notificator.notifyEapDownloaded() }
          else -> notificator.notifySubscribedToEap()
        }
      }

      override fun onCancel() {
        notificator.notifySubscribedToEap()
      }

      override fun onThrowable(error: Throwable) {
        notificator.notifySubscribedToEap()
      }
    }.queue()
  }

  private fun updatePlugin(project: Project?, downloader: PluginDownloader, onSuccess: (updated: Boolean) -> Unit) {
    val notificator = VimPlugin.getNotifications(project)
    return object : Task.Backgroundable(null, "Plugin Updates", true, PerformInBackgroundOption.DEAF) {
      private var updated = false
      override fun run(indicator: ProgressIndicator) {
        updated = UpdateInstaller.installPluginUpdates(listOf(downloader), indicator)
      }

      override fun onSuccess() {
        onSuccess(updated)
      }

      override fun onCancel() {
        notificator.notifyFailedToDownloadEap()
      }

      override fun onThrowable(error: Throwable) {
        notificator.notifyFailedToDownloadEap()
      }
    }.queue()
  }
}
