/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.format

import com.intellij.openapi.editor.impl.EditorId
import com.intellij.platform.rpc.RemoteApiProviderService
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import org.jetbrains.annotations.ApiStatus

@Rpc
@ApiStatus.Internal
interface FormatRemoteApi : RemoteApi<Unit> {
  /**
   * Auto-indents the lines covered by [startOffset]..[endOffset]. Several ranges mean several calls,
   * which the caller must issue bottom-to-top so that one range doesn't shift the ranges above it.
   */
  suspend fun format(editorId: EditorId, startOffset: Int, endOffset: Int)

  /**
   * The indent the language gives to the line at [offset], as the IDE works it out for a new line. Null if the
   * language has no formatter, or if the indent of the line is not to be adjusted.
   */
  suspend fun lineIndent(editorId: EditorId, offset: Int): String?

  companion object {
    @JvmStatic
    suspend fun getInstance(): FormatRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<FormatRemoteApi>())
    }
  }
}
