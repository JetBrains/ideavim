/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor

/**
 * Registers [BookmarkRemoteApiImpl] as the backend RPC handler for [BookmarkRemoteApi].
 * Registered via `platform.rpc.backend.remoteApiProvider` extension point in ideavim-backend.xml.
 */
internal class BookmarkRemoteApiProvider : RemoteApiProvider {
  override fun RemoteApiProvider.Sink.remoteApis() {
    remoteApi(remoteApiDescriptor<BookmarkRemoteApi>()) {
      BookmarkRemoteApiImpl()
    }
  }
}
