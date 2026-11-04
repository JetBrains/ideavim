/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.change

import com.intellij.openapi.editor.impl.EditorId
import com.maddyhome.idea.vim.group.rpc.VimRemoteApiResolver
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for undo on the backend in split mode.
 *
 * [startUndoMark]/[finishUndoMark] register StartMarkAction/FinishMarkAction
 * on the backend's UndoManager, bracketing multiple document changes into a
 * single undo step.
 *
 * [undo]/[redo] run undo/redo on the backend's UndoManager for frontends
 * without speculative undo (e.g. Rider), where the undo stack lives on the backend.
 */
@Rpc
@ApiStatus.Internal
interface ChangeRemoteApi : RemoteApi<Unit> {
  /** Register a StartMarkAction on the backend's UndoManager. */
  suspend fun startUndoMark(editorId: EditorId, commandName: String)

  /** Register a FinishMarkAction on the backend's UndoManager. */
  suspend fun finishUndoMark(editorId: EditorId)

  /**
   * Undo the last change in the editor, with the caret movement made after it as part of the same step.
   * Returns false if there is nothing to undo.
   */
  suspend fun undo(editorId: EditorId): Boolean

  /**
   * Redo the last undone change in the editor, with the caret movement as part of the same step.
   * Returns false if there is nothing to redo.
   */
  suspend fun redo(editorId: EditorId): Boolean

  companion object {
    @JvmStatic
    suspend fun getInstance(): ChangeRemoteApi {
      return VimRemoteApiResolver.resolve(remoteApiDescriptor<ChangeRemoteApi>())
    }
  }
}
