/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.util.ui.StartupUiUtil
import com.maddyhome.idea.vim.group.file.FileRemoteApi

class WhatsNewHelper {

  companion object {
    fun showWhatsNew(project: Project?, version: String) {
      if (project == null) return
      val theme = if (StartupUiUtil.isDarkTheme) "dark" else "light"
      val html = rpc(project) { getVersionHtml(theme, version) } ?: return
      HTMLEditorProvider.openEditor(project, "What's new in $version", html)
    }

    private suspend fun getVersionHtml(theme: String, version: String): String? =
      (loadHtml("whatsnew-$version.html") ?: loadHtml("whatsnew-tbr.html"))
        ?.replace("__THEME__", theme)
        ?.replace("__VERSION__", version)

    /**
     * Read on the backend: in split mode the frontend cannot read the plugin's bundled resources.
     */
    private suspend fun loadHtml(resource: String): String? =
      FileRemoteApi.getInstance().readResource(resource)
  }
}
