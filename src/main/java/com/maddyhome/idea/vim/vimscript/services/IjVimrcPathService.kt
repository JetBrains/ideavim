/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.maddyhome.idea.vim.api.injector
import java.nio.file.Path
import kotlin.io.path.Path

internal class VimrcPathState : BaseState() {
  var vimrcPath: String? by string()
}

@State(
  name = "VimrcFilePath",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class IjVimrcPathService :
  SimplePersistentStateComponent<VimrcPathState>(VimrcPathState()),
  VimrcPathService {

  override var vimrcPath: String
    get() = state.vimrcPath ?: ""
    set(value) {
      state.vimrcPath = value.trim().ifEmpty { null }
    }

  override fun resolvePath(path: String): Path? {
    val trimmed = path.trim()
    if (trimmed.isEmpty()) return null
    // IDE macros must be expanded first: they use the `$NAME$` syntax, and the Vim expansion would otherwise treat
    // `$USER_HOME` as an unknown environment variable and replace it with an empty string
    val withMacrosExpanded = PathMacroManager.getInstance(ApplicationManager.getApplication()).expandPath(trimmed)
    return Path(injector.pathExpansion.expandPath(withMacrosExpanded))
  }
}
