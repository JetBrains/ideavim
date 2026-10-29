/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.intellij.openapi.editor.impl.EditorId
import com.intellij.platform.project.ProjectId
import com.maddyhome.idea.vim.group.rpc.VimRemoteApiResolver
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for jump operations that require backend-only APIs.
 * Called by [VimJumpServiceSplitClient] in split mode.
 *
 * [includeCurrentCommandAsNavigation] forwards [IdeDocumentHistory] calls to the backend.
 *
 * [diffJumpAt] and [showDiffJump] work with diff tabs, which live on the backend in split mode. The frontend only shows
 * them, with editors that mirror the backend's ones and share their [EditorId].
 */
@Rpc
@ApiStatus.Internal
interface JumpRemoteApi : RemoteApi<Unit> {
  suspend fun includeCurrentCommandAsNavigation(projectId: ProjectId?)

  /** The jump to [line] and [column] of the editor [editorId], or null if the editor is not part of a diff tab */
  suspend fun diffJumpAt(editorId: EditorId, line: Int, column: Int): DiffJump?

  /** Brings back the diff tab of [jump] and moves its caret to the jump. False if no open diff can show it */
  suspend fun showDiffJump(projectId: ProjectId, jump: DiffJump): Boolean

  companion object {
    @JvmStatic
    suspend fun getInstance(): JumpRemoteApi {
      return VimRemoteApiResolver.resolve(remoteApiDescriptor<JumpRemoteApi>())
    }
  }
}
