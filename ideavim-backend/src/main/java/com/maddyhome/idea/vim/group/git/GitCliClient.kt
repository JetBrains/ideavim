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
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.isExecutable

/**
 * [GitClient] that runs the git executable given by [GitExecutableLocator], falling back to `git` on PATH.
 */
internal class GitCliClient(private val project: Project) : GitClient {

  override val executable: String
    get() = GitExecutableLocator.EP_NAME.extensionList.firstNotNullOfOrNull { it.findExecutable(project) } ?: GIT

  override fun isExecutableFound(): Boolean {
    val git = executable
    return if (isBareName(git)) isOnPath(git) else Path(git).isExecutable()
  }

  /**
   * Resolves `--show-cdup` against [directory] rather than using `--show-toplevel`, which resolves symlinks, e.g.
   * `/tmp` to `/private/tmp` on macOS. Paths built from it then match the files already open in the IDE.
   */
  override fun findWorkTree(directory: String): String? {
    val output = execute(directory, listOf("rev-parse", "--show-cdup"))
    if (output.exitCode != 0) return null
    val pathToRoot = output.stdout.firstOrNull().orEmpty().trim()
    return FileUtil.toSystemIndependentName(Path(directory).resolve(pathToRoot).normalize().toString())
  }

  override fun run(workTree: String, args: List<String>): GitOutput = execute(workTree, args)

  private fun execute(workDirectory: String, args: List<String>): GitOutput {
    val git = executable
    val commandLine = GeneralCommandLine(git)
      .withParameters(args)
      .withCharset(StandardCharsets.UTF_8)
      .withWorkingDirectory(Path(workDirectory))
    val output = try {
      runCancellably(CapturingProcessHandler(commandLine))
    } catch (e: ExecutionException) {
      throw GitCannotExecuteException(git, e)
    }
    if (output.isCancelled) throw ProcessCanceledException()
    return GitOutput(output.exitCode, output.stdoutLines, output.stderr)
  }

  /**
   * With a progress indicator, cancelling it kills the process.
   */
  private fun runCancellably(handler: CapturingProcessHandler): ProcessOutput {
    val indicator = ProgressManager.getGlobalProgressIndicator() ?: return handler.runProcess()
    return handler.runProcessWithProgressIndicator(indicator)
  }

  private companion object {
    const val GIT = "git"

    /**
     * A bare name like `git` is looked up on PATH, anything else is a path to the executable.
     */
    fun isBareName(executable: String): Boolean = '/' !in executable && '\\' !in executable

    fun isOnPath(executable: String): Boolean = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(executable) != null
  }
}
