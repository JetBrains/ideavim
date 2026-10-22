/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.maddyhome.idea.vim.group.git.GitClient
import com.maddyhome.idea.vim.group.git.GitExecutableStatus
import com.maddyhome.idea.vim.group.git.GitOutput

/**
 * [GitClient] that returns canned git output and records what was run.
 */
class FakeGitClient : GitClient {
  override var executable = "git"

  var executableStatus = GitExecutableStatus.OK

  /** Work tree returned for every directory, or null to act as if there's no repository */
  var workTree: String? = "/src"

  var output = GitOutput(exitCode = 1, stdout = emptyList(), stderr = "")

  var lastWorkTree: String? = null
    private set
  var lastArgs: List<String>? = null
    private set

  override fun checkExecutable(): GitExecutableStatus = executableStatus

  override fun findWorkTree(directory: String): String? = workTree

  override fun run(workTree: String, args: List<String>): GitOutput {
    lastWorkTree = workTree
    lastArgs = args
    return output
  }

  fun respondWith(vararg stdout: String) {
    output = GitOutput(exitCode = if (stdout.isEmpty()) 1 else 0, stdout = stdout.toList(), stderr = "")
  }
}
