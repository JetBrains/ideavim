/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.format

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor

/**
 * Registers [FormatRemoteApiImpl] as the backend RPC handler for [FormatRemoteApi].
 * Registered via `platform.rpc.backend.remoteApiProvider` extension point in ideavim-backend.xml.
 */
internal class FormatRemoteApiProvider : RemoteApiProvider {
  override fun RemoteApiProvider.Sink.remoteApis() {
    remoteApi(remoteApiDescriptor<FormatRemoteApi>()) {
      FormatRemoteApiImpl()
    }
  }
}
