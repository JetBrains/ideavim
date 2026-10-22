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
 * In newer IDEs both modules load, but only one can resolve [GitExecutableManager] from its own
 * classloader. The other returns null, so [GitCliClient] uses the first locator that answers.
 */
internal class GitPluginExecutableLocator : GitExecutableLocator {

  private val gitPluginResolvable: Boolean by lazy {
    try {
      // `initialize = false`: we only need to know the class is reachable from this classloader
      Class.forName("git4idea.config.GitExecutableManager", false, javaClass.classLoader)
      true
    } catch (_: Throwable) {
      false
    }
  }

  override fun findExecutable(project: Project): String? {
    if (!gitPluginResolvable) return null
    return GitExecutableManager.getInstance().getPathToGit(project)
  }
}
