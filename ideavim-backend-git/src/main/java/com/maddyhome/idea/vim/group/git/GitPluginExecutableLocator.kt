/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.git

import com.intellij.openapi.project.Project
import git4idea.config.GitExecutableManager

/**
 * Uses the git executable configured in Settings | Version Control | Git, or the one the Git plugin detected.
 *
 * This exact class ships in two content modules that share this source:
 * - `IdeaVIM.ideavim-backend-git` depends on the `intellij.vcs.git.backend` module, where
 *   [GitExecutableManager] lives in newer IDEs.
 * - `IdeaVIM.ideavim-backend-git-legacy` depends on the `Git4Idea` plugin, whose main module
 *   contains [GitExecutableManager] in 2026.1.
 *
 * In newer IDEs both modules load, and both may resolve [GitExecutableManager], each through its own
 * classloader. Only the copy the service was registered with can fetch it; the other gets a
 * [ClassCastException] from the service lookup and returns null, so [GitCliClient] uses the
 * first locator that answers.
 */
internal class GitPluginExecutableLocator : GitExecutableLocator {

  private val gitPluginUsable: Boolean by lazy {
    try {
      GitExecutableManager.getInstance()
      true
    } catch (_: LinkageError) {
      false
    } catch (_: ClassCastException) {
      false
    }
  }

  override fun findExecutable(project: Project): String? {
    if (!gitPluginUsable) return null
    return GitExecutableManager.getInstance().getPathToGit(project)
  }
}
