/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.comment

import com.intellij.openapi.editor.impl.EditorId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

/**
 * RPC interface for commenting operations on the backend.
 *
 * In split mode, document modifications must happen on the backend so that
 * [com.intellij.openapi.command.CommandProcessor] groups them as a single undo step.
 * If done on the frontend, each document change syncs as a separate delta and creates
 * individual undo entries on the backend.
 *
 * Called from [CommentaryExtension][com.maddyhome.idea.vim.extension.commentary.CommentaryExtension].
 */
@Rpc
@ApiStatus.Internal
interface CommentaryRemoteApi : RemoteApi<Unit> {
  /**
   * Toggles line comments for the given line range.
   *
   * Executes on the backend inside a [CommandProcessor.executeCommand] so all document
   * modifications are grouped as a single undo step.
   *
   * @param editorId the editor to operate on
   * @param startLine 0-based start line
   * @param endLine 0-based end line (inclusive)
   */
  suspend fun toggleLineComment(editorId: EditorId, startLine: Int, endLine: Int, caretOffset: Int)

  /**
   * Toggles block comments for the given offset range.
   *
   * Tries block comment first, falls back to line comment.
   *
   * @param editorId the editor to operate on
   * @param startOffset start offset of the range
   * @param endOffset end offset of the range
   * @param caretOffset offset to place the caret after commenting (-1 to skip)
   */
  suspend fun toggleBlockComment(editorId: EditorId, startOffset: Int, endOffset: Int, caretOffset: Int)

  companion object {
    @JvmStatic
    suspend fun getInstance(): CommentaryRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<CommentaryRemoteApi>())
    }
  }
}
