/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.mark.Jump
import kotlinx.coroutines.runBlocking

/**
 * Thin-client jump service for split (Remote Development) mode.
 *
 * Extends [VimJumpServiceBase] which provides in-memory jump list management
 * for jumps triggered by user actions on the frontend (e.g., `50G`, `/pattern`).
 *
 * [includeCurrentCommandAsNavigation] delegates to the backend via RPC, where
 * [IdeDocumentHistory] is available.
 *
 * Backend jumps (IDE navigation events collected by [JumpsListener]) are merged
 * into [projectToJumps] before any read operation, so both `:jumps` and
 * `Ctrl-O`/`Ctrl-I` navigation see the complete picture.
 */
internal class VimJumpServiceSplitClient : VimJumpServiceBase() {
  override var lastJumpTimeStamp: Long = 0

  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    rpc { includeCurrentCommandAsNavigation(editor.projectId) }
  }

  override fun getJump(projectId: String, count: Int): Jump? {
    syncBackendJumps(projectId)
    return super.getJump(projectId, count)
  }

  override fun getJumps(projectId: String): List<Jump> {
    syncBackendJumps(projectId)
    return super.getJumps(projectId)
  }

  /**
   * Merges backend jumps (from [JumpsListener] via RPC) into [projectToJumps].
   * Deduplicates by filepath+line, keeping existing local entries.
   */
  private fun syncBackendJumps(projectId: String) {
    val backendJumps = rpc { getListenerJumps(projectId) }
      .map { Jump(it.line, it.col, it.filepath, it.protocol) }
    val localJumps = projectToJumps.getOrPut(projectId) { mutableListOf() }
    val seen = localJumps.map { it.filepath to it.line }.toSet()
    for (jump in backendJumps) {
      if ((jump.filepath to jump.line) !in seen) {
        localJumps.add(jump)
      }
    }
    while (localJumps.size > SAVE_JUMP_COUNT) {
      localJumps.removeFirst()
    }
  }

  private fun <T> rpc(block: suspend JumpRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { JumpRemoteApi.getInstance().block() }
  }
}
