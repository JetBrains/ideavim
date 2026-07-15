/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import com.maddyhome.idea.vim.api.VimExternalOpener

/**
 * IntelliJ implementation of [VimExternalOpener] for the `gx` command.
 *
 * Hands the target (a URL or a file path) to the operating system's default handler, so a URL opens
 * in the default browser and a file opens with its associated application. This mirrors Neovim's
 * `vim.ui.open` / `_get_open_cmd()`
 */
internal class IjVimExternalOpener : VimExternalOpener {
  override fun open(target: String, viewer: String?) {
    val command = if (viewer != null) viewerCommand(viewer) + target else osOpenCommand()?.plus(target)
    if (command == null) {
      LOG.warn("gx: no external open handler found for this platform")
      return
    }
    try {
      GeneralCommandLine(command).createProcess()
    } catch (e: ExecutionException) {
      LOG.warn("gx: failed to open '$target'", e)
    }
  }

  /**
   * Splits a `g:netrw_browsex_viewer` value into its executable and arguments. `gx` then appends the
   * target as the final argument, matching netrw's `viewer viewopt fname`.
   */
  private fun viewerCommand(viewer: String): List<String> = viewer.trim().split(WHITESPACE)

  private fun osOpenCommand(): List<String>? = when {
    SystemInfo.isMac -> listOf("open")
    SystemInfo.isWindows -> listOf("cmd.exe", "/c", "start", "")
    isExecutable("xdg-open") -> listOf("xdg-open")
    isExecutable("wslview") -> listOf("wslview")
    isExecutable("explorer.exe") -> listOf("explorer.exe")
    isExecutable("lemonade") -> listOf("lemonade", "open")
    else -> null
  }

  private fun isExecutable(name: String): Boolean =
    PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(name) != null

  companion object {
    private val LOG = logger<IjVimExternalOpener>()
    private val WHITESPACE = Regex("""\s+""")
  }
}
