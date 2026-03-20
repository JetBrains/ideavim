/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.PlatformUtils

/**
 * Executes a suspend [block] as a blocking RPC call.
 *
 * Uses [runWithModalProgressBlocking] instead of `runBlocking` because
 * `runBlocking` on EDT is de-facto prohibited by the IntelliJ platform.
 *
 * Works in both monolith and split mode:
 * - **Monolith**: RPC resolves to the backend handler locally.
 * - **Split**: RPC sends to the backend process.
 */
internal fun <T> rpc(project: Project? = null, block: suspend () -> T): T {
  val resolvedProject = project
    ?: ProjectManager.getInstance().openProjects.firstOrNull()
    ?: error("No open project available for RPC call")
  return runWithModalProgressBlocking(resolvedProject, "") { block() }
}

/**
 * Executes a suspend [block] only when running in split mode (JetBrains Client).
 * In monolith mode this is a no-op.
 *
 * Use this for operations that must only run on the backend in split mode,
 * e.g. registering undo marks that would interfere with the local UndoManager
 * if executed in the same process.
 */
internal fun rpcSplitModeOnly(project: Project? = null, block: suspend () -> Unit) {
  if (PlatformUtils.isJetBrainsClient()) {
    rpc(project, block)
  }
}
