/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.change

import com.intellij.openapi.editor.impl.EditorId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for undo grouping on the backend in split mode.
 *
 * [startUndoMark]/[finishUndoMark] register StartMarkAction/FinishMarkAction
 * on the backend's UndoManager, bracketing multiple document changes into a
 * single undo step.
 */
@Rpc
@ApiStatus.Internal
interface ChangeRemoteApi : RemoteApi<Unit> {
  /** Register a StartMarkAction on the backend's UndoManager. */
  suspend fun startUndoMark(editorId: EditorId, commandName: String)

  /** Register a FinishMarkAction on the backend's UndoManager. */
  suspend fun finishUndoMark(editorId: EditorId)

  companion object {
    @JvmStatic
    suspend fun getInstance(): ChangeRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<ChangeRemoteApi>())
    }
  }
}
