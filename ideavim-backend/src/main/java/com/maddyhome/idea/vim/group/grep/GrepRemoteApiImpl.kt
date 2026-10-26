/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.grep

import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.maddyhome.idea.vim.group.findVirtualFile
import com.maddyhome.idea.vim.group.git.GitCannotExecuteException
import com.maddyhome.idea.vim.group.git.GitClient
import com.maddyhome.idea.vim.group.git.GitOutput
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RPC handler for [GrepRemoteApi]. Turns every way `:Ggrep` can fail into a message for the user.
 *
 * Note: Must NOT use [com.maddyhome.idea.vim.api.injector] — it is not initialized on the backend
 * in split mode. Uses platform [ProjectId.findProjectOrNull] instead.
 */
internal class GrepRemoteApiImpl : GrepRemoteApi {

  override suspend fun grep(
    projectId: ProjectId,
    filepath: String?,
    protocol: String?,
    args: List<String>,
  ): GrepResult = withContext(Dispatchers.IO) {
    try {
      val project = projectId.findProjectOrNull() ?: throw GrepFailure(notInRepository(file = null))
      val file = filepath?.let { findVirtualFile(it, protocol) }
      // Lets git be killed when the user cancels a slow search
      val matches = coroutineToIndicator { _ -> GitGrep(GitClient.getInstance(project)).search(file, project, args) }
      GrepResult(matches = matches)
    } catch (e: GrepFailure) {
      GrepResult(errorMessage = e.message)
    } catch (_: GitCannotExecuteException) {
      GrepResult(errorMessage = EngineMessageHelper.message("fugitive.error.git.cannot.execute"))
    }
  }
}

/**
 * Runs `git grep` the way fugitive's `s:GrepSubcommand` does: in the work tree of the current
 * file, with paths relative to the work tree root.
 */
private class GitGrep(private val git: GitClient) {

  fun search(file: VirtualFile?, project: Project, args: List<String>): List<GrepMatch> {
    requireGitFound()
    val directory = file?.parent ?: project.guessProjectDir() ?: throw GrepFailure(notInRepository(file))
    val workTree = git.findWorkTree(directory.path) ?: throw GrepFailure(notInRepository(file))
    val output = git.run(workTree, GREP_COMMAND + args)
    if (output.isError()) throw GrepFailure(output.stderr.trim())
    return GitGrepOutputParser.parse(output.stdout, workTree, directory.fileSystem.protocol)
  }

  private fun requireGitFound() {
    if (!git.isExecutableFound()) {
      throw GrepFailure(EngineMessageHelper.message("fugitive.error.git.not.found", git.executable))
    }
  }

  /**
   * `git grep` exits with 1 when nothing matches, which isn't an error.
   */
  private fun GitOutput.isError(): Boolean = exitCode > 1

  private companion object {
    val GREP_COMMAND = listOf("-c", "core.quotePath=false", "grep", "--no-color", "--full-name", "-n", "--column")
  }
}

private class GrepFailure(override val message: String) : Exception(message)

/**
 * Like fugitive, tells apart a file outside a repository from a working directory outside one.
 */
private fun notInRepository(file: VirtualFile?): String = if (file != null) {
  EngineMessageHelper.message("fugitive.error.file.not.in.repository")
} else {
  EngineMessageHelper.message("fugitive.error.directory.not.in.repository")
}
