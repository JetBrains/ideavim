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
  override fun open(target: String) {
    val command = openCommand()
    if (command == null) {
      LOG.warn("gx: no external open handler found for this platform")
      return
    }
    try {
      GeneralCommandLine(command + target).createProcess()
    } catch (e: ExecutionException) {
      LOG.warn("gx: failed to open '$target'", e)
    }
  }

  private fun openCommand(): List<String>? = when {
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
  }
}
