/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.git

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * Runs the `git` executable on the backend.
 *
 * It's an interface so tests can replace it with a fake that needs neither git installed
 * nor a repository on disk. The real implementation lives in ideavim-backend.
 */
@ApiStatus.Internal
interface GitClient {
  /**
   * The git executable that [run] starts: a bare name looked up on PATH, or a path.
   */
  val executable: String

  /**
   * Checks that [executable] exists and can be started.
   */
  fun checkExecutable(): GitExecutableStatus

  /**
   * Returns the root of the work tree containing [directory] (`git rev-parse --show-toplevel`),
   * or null if [directory] is not inside a Git work tree.
   */
  fun findWorkTree(directory: String): String?

  /**
   * Runs `git [args]` with [workTree] as the working directory.
   */
  fun run(workTree: String, args: List<String>): GitOutput

  companion object {
    @JvmStatic
    fun getInstance(project: Project): GitClient = project.service()
  }
}

@ApiStatus.Internal
enum class GitExecutableStatus {
  OK,
  NOT_FOUND,
  CANNOT_EXECUTE,
}

@ApiStatus.Internal
data class GitOutput(
  val exitCode: Int,
  val stdout: List<String>,
  val stderr: String,
)
