/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.platform.project.ProjectId
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimJumpServiceBase
import com.maddyhome.idea.vim.group.CoroutineScopeProvider
import kotlinx.coroutines.runBlocking

/**
 * Thin-client jump service for split (Remote Development) mode.
 *
 * Extends [VimJumpServiceBase] which provides in-memory jump list management.
 * Jump events from the backend [JumpsListener] arrive via [JUMP_REMOTE_TOPIC]
 * and are handled by [JumpRemoteTopicListener] which calls [addJump]/[removeJump].
 *
 * [includeCurrentCommandAsNavigation] delegates to the backend via RPC, where
 * [IdeDocumentHistory] is available.
 */
internal class VimJumpServiceSplitClient : VimJumpServiceBase() {
  override var lastJumpTimeStamp: Long = 0

  override fun includeCurrentCommandAsNavigation(editor: VimEditor) {
    val projectId = deserializeProjectId(editor.projectId) ?: return
    rpc { includeCurrentCommandAsNavigation(projectId) }
  }

  private fun deserializeProjectId(projectId: String): ProjectId? {
    return try {
      ProjectId.deserializeFromString(projectId)
    } catch (_: Exception) {
      null
    }
  }

  private fun <T> rpc(block: suspend JumpRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { JumpRemoteApi.getInstance().block() }
  }
}
