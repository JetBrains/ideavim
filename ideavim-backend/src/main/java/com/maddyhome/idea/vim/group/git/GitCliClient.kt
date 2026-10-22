/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.git

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.isExecutable

/**
 * [GitClient] that runs the git executable given by [GitExecutableLocator], falling back to `git` on PATH.
 */
internal class GitCliClient(private val project: Project) : GitClient {

  override val executable: String
    get() = GitExecutableLocator.EP_NAME.extensionList.firstNotNullOfOrNull { it.findExecutable(project) } ?: GIT

  override fun checkExecutable(): GitExecutableStatus {
    val executable = executable
    if (!exists(executable)) return GitExecutableStatus.NOT_FOUND
    val output = execute(executable, workDirectory = null, listOf("--version"))
    return if (output?.exitCode == 0) GitExecutableStatus.OK else GitExecutableStatus.CANNOT_EXECUTE
  }

  override fun findWorkTree(directory: String): String? {
    val output = execute(executable, directory, listOf("rev-parse", "--show-toplevel")) ?: return null
    if (output.exitCode != 0) return null
    return output.stdout.firstOrNull()?.takeIf { it.isNotBlank() }
  }

  override fun run(workTree: String, args: List<String>): GitOutput {
    return execute(executable, workTree, args) ?: GitOutput(exitCode = -1, stdout = emptyList(), stderr = "Cannot execute Git")
  }

  /**
   * A bare name like `git` is looked up on PATH, anything else is treated as a file path.
   */
  private fun exists(executable: String): Boolean {
    val isBareName = '/' !in executable && '\\' !in executable
    return if (isBareName) {
      PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(executable) != null
    } else {
      Path(executable).isExecutable()
    }
  }

  private fun execute(executable: String, workDirectory: String?, args: List<String>): GitOutput? {
    val commandLine = GeneralCommandLine(executable)
      .withParameters(args)
      .withCharset(StandardCharsets.UTF_8)
    if (workDirectory != null) commandLine.withWorkingDirectory(Path(workDirectory))
    return try {
      val output = CapturingProcessHandler(commandLine).runProcess()
      GitOutput(output.exitCode, output.stdoutLines, output.stderr)
    } catch (e: ExecutionException) {
      logger.warn("Cannot run git ${args.joinToString(" ")}", e)
      null
    }
  }

  private companion object {
    const val GIT = "git"
    val logger = logger<GitCliClient>()
  }
}
