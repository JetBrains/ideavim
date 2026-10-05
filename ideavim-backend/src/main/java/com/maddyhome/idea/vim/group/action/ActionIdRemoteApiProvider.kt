/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.action

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor

/**
 * Registers [ActionIdRemoteApiImpl] as the backend RPC handler for [ActionIdRemoteApi].
 * Registered via `platform.rpc.backend.remoteApiProvider` extension point in ideavim-backend.xml.
 */
internal class ActionIdRemoteApiProvider : RemoteApiProvider {
  override fun RemoteApiProvider.Sink.remoteApis() {
    remoteApi(remoteApiDescriptor<ActionIdRemoteApi>()) {
      ActionIdRemoteApiImpl()
    }
  }
}
