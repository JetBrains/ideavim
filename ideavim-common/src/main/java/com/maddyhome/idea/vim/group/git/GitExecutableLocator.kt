/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.git

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * Tells [GitClient] which git executable to run.
 *
 * Implemented in ideavim-backend-git on top of the Git plugin, so the path configured in
 * Settings | Version Control | Git is used. Without the Git plugin `git` is looked up on PATH.
 */
@ApiStatus.Internal
interface GitExecutableLocator {
  /**
   * Returns the path to the git executable, or null if this locator doesn't know it.
   */
  fun findExecutable(project: Project): String?

  companion object {
    @JvmField
    val EP_NAME: ExtensionPointName<GitExecutableLocator> = ExtensionPointName.create("IdeaVIM.gitExecutableLocator")
  }
}
