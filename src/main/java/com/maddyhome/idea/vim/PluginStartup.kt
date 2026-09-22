/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.IjVimEnabler
import com.maddyhome.idea.vim.ui.JoinEap
import com.maddyhome.idea.vim.ui.JoinEap.EAP_LINK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Alex Plate
 */
internal class PluginStartup : ProjectActivity/*, LightEditCompatible*/ {

  private var firstInitializationOccurred = false

  // TODO
  // We should migrate to some solution from https://plugins.jetbrains.com/docs/intellij/plugin-components.html#application-startup
  // If you'd like to add a new code here, please consider using one of the things described there.
  override suspend fun execute(project: Project) {
    withContext(Dispatchers.EDT) {
      VimPlugin.getSearchIfCreated()?.clearSearchHighlight()
    }

    if (firstInitializationOccurred) return
    firstInitializationOccurred = true

    if (!VimPlugin.getVimState().wasSubscribedToEAPAutomatically && ApplicationManager.getApplication().isEAP && !JoinEap.eapActive()) {
      VimPlugin.getVimState().wasSubscribedToEAPAutomatically = true
      UpdateSettings.getInstance().storedPluginHosts += EAP_LINK
    }

    // This code should be executed once
    VimPlugin.getInstance().initialize()

    (injector.enabler as IjVimEnabler).ideOpened()
  }
}
