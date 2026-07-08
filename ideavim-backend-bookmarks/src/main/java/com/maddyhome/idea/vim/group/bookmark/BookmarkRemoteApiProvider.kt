/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.platform.rpc.backend.RemoteApiProvider
import fleet.rpc.remoteApiDescriptor

/**
 * Registers [BookmarkRemoteApiImpl] as the backend RPC handler for [BookmarkRemoteApi].
 *
 * This exact class ships in two content modules that share this source:
 * - `IdeaVIM.ideavim-backend-bookmarks` depends on `intellij.platform.bookmarks` and is only
 *   enabled on IDEs where that module exists (2026.2+, where the bookmark classes were moved out
 *   of the monolith into a separate plugin module).
 * - `IdeaVIM.ideavim-backend-bookmarks-core` has no such dependency and is enabled everywhere;
 *   on 2026.1 and earlier the bookmark classes are part of platform core.
 *
 * To keep the two from double-registering, each instance registers only when the bookmark classes
 * are actually resolvable from *its own* classloader. On 2026.2+ that is true only for the
 * module-backed variant; on <=2026.1 only for the core-backed variant. Exactly one registers per
 * IDE, and neither ever hits [NoClassDefFoundError] because the impl is instantiated lazily and
 * only after this guard passes.
 */
internal class BookmarkRemoteApiProvider : RemoteApiProvider {
  override fun RemoteApiProvider.Sink.remoteApis() {
    if (!bookmarkClassesResolvable()) return
    remoteApi(remoteApiDescriptor<BookmarkRemoteApi>()) {
      BookmarkRemoteApiImpl()
    }
  }

  private fun bookmarkClassesResolvable(): Boolean = try {
    // `initialize = false`: we only need to know the class is reachable from this classloader,
    // not to trigger its static initializer.
    Class.forName("com.intellij.ide.bookmark.BookmarksManager", false, javaClass.classLoader)
    true
  } catch (_: Throwable) {
    false
  }
}
