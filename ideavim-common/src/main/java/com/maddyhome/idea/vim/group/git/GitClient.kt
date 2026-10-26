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
   * Checks that [executable] is on PATH or exists, without starting it.
   * Whether it actually starts shows only when [findWorkTree] or [run] is called.
   */
  fun isExecutableFound(): Boolean

  /**
   * Returns the root of the work tree containing [directory], in the same path form as [directory] (symlinks are not
   * resolved), or null if [directory] is not inside a Git work tree.
   *
   * @throws GitCannotExecuteException if git can't be started
   */
  fun findWorkTree(directory: String): String?

  /**
   * Runs `git [args]` with [workTree] as the working directory.
   *
   * @throws GitCannotExecuteException if git can't be started
   */
  fun run(workTree: String, args: List<String>): GitOutput

  companion object {
    @JvmStatic
    fun getInstance(project: Project): GitClient = project.service()
  }
}

@ApiStatus.Internal
class GitCannotExecuteException(executable: String, cause: Throwable? = null) :
  Exception("Cannot execute $executable", cause)

@ApiStatus.Internal
data class GitOutput(
  val exitCode: Int,
  val stdout: List<String>,
  val stderr: String,
)
