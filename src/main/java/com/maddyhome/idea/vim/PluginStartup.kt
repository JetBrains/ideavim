/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.initInjector
import com.maddyhome.idea.vim.ui.JoinEap
import com.maddyhome.idea.vim.ui.JoinEap.EAP_LINK

/**
 * @author Alex Plate
 */
internal class PluginStartup : ProjectActivity/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  // TODO
  // We should migrate to some solution from https://plugins.jetbrains.com/docs/intellij/plugin-components.html#application-startup
  // If you'd like to add a new code here, please consider using one of the things described there.
  override suspend fun execute(project: Project) {
    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    if (!VimPlugin.getVimState().wasSubscibedToEAPAutomatically && ApplicationManager.getApplication().isEAP && !JoinEap.eapActive()) {
      VimPlugin.getVimState().wasSubscibedToEAPAutomatically = true
      UpdateSettings.getInstance().storedPluginHosts += EAP_LINK
    }

    // This code should be executed once
    VimPlugin.getInstance().initialize()

    // Uninstall survey. Should be registered once for all projects
    PluginStateManager.addStateListener(object : PluginStateListener {
      override fun install(p0: IdeaPluginDescriptor) {/*Nothing*/
      }

      override fun uninstall(descriptor: IdeaPluginDescriptor) {
        val pluginId = VimPlugin.getPluginId()
        // This event is called for both uninstall and update. There is no proper way to distinguish these two events.
        // In order not to show the form for the update, we check if the new version is available. If so,
        //   this may be an update (and may not), and we don't show the form.
        if (descriptor.pluginId == pluginId && !InstalledPluginsState.getInstance().hasNewerVersion(pluginId)) {
          BrowserUtil.open("https://surveys.jetbrains.com/s3/ideavim-uninstall-feedback")
        }
      }
    })
  }
}

// This is a temporal workaround for VIM-2487
internal class PyNotebooksCloseWorkaround : ProjectManagerListener {
  override fun projectClosingBeforeSave(project: Project) {
    initInjector()
    // TODO: Confirm context in CWM scenario
    if (injector.globalIjOptions().closenotebooks) {
      injector.editorGroup.getEditors().forEach { vimEditor ->
        val editor = (vimEditor as IjVimEditor).editor
        val virtualFile = EditorHelper.getVirtualFile(editor)
        if (virtualFile?.extension == "ipynb") {
          val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
          fileEditorManager.closeFile(virtualFile)
        }
      }
    }
  }
}
