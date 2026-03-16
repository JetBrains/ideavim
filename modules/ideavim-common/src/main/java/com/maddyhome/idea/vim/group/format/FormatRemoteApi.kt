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
  suspend fun format(editorId: EditorId, startOffsets: List<Int>, endOffsets: List<Int>)

  companion object {
    @JvmStatic
    suspend fun getInstance(): FormatRemoteApi {
      return RemoteApiProviderService.resolve(remoteApiDescriptor<FormatRemoteApi>())
    }
  }
}