/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.grep

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.project.ProjectId
import com.intellij.platform.project.findProjectOrNull
import com.maddyhome.idea.vim.group.findVirtualFile
import com.maddyhome.idea.vim.group.git.GitClient
import com.maddyhome.idea.vim.group.git.GitExecutableStatus
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RPC handler for [GrepRemoteApi]. Runs `git grep` the way fugitive's `s:GrepSubcommand` does:
 * in the work tree of the current file, with paths relative to the work tree root.
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
      GitGrep(GitClient.getInstance(project)).run(file, project, args)
    } catch (e: GrepFailure) {
      GrepResult(errorMessage = e.message)
    }
  }
}

private class GitGrep(private val git: GitClient) {

  fun run(file: VirtualFile?, project: Project, args: List<String>): GrepResult {
    requireExecutable()
    val directory = file?.parent ?: project.guessProjectDir()
    val workTree = directory?.let { git.findWorkTree(it.path) } ?: throw GrepFailure(notInRepository(file))
    val output = git.run(workTree, GREP_COMMAND + args)
    // Exit code 1 means no matches
    if (output.exitCode !in 0..1) throw GrepFailure(output.stderr.trim())
    val protocol = directory.fileSystem.protocol
    return GrepResult(matches = GitGrepOutputParser.parse(output.stdout, workTree, protocol))
  }

  private fun requireExecutable() {
    val message = when (git.checkExecutable()) {
      GitExecutableStatus.OK -> return
      GitExecutableStatus.NOT_FOUND -> EngineMessageHelper.message("fugitive.error.git.not.found", git.executable)
      GitExecutableStatus.CANNOT_EXECUTE -> EngineMessageHelper.message("fugitive.error.git.cannot.execute")
    }
    throw GrepFailure(message)
  }

  private companion object {
    val GREP_COMMAND = listOf("grep", "--no-color", "--full-name", "-n", "--column")
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
