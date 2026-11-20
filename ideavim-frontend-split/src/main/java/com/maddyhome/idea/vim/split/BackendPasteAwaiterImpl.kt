/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationInfo
import com.jetbrains.rdclient.actions.base.ActionContinuationHolder
import com.jetbrains.rdclient.actions.base.BACKEND_ACTION_CONTINUATION_KEY
import com.maddyhome.idea.vim.group.copy.BackendPasteAwaiter

internal class BackendPasteAwaiterImpl : BackendPasteAwaiter {
  override fun paste(context: DataContext, paste: (DataContext) -> Unit): (suspend () -> Unit)? {
    // Before 2026.3 the backend pastes before the clipboard put back after the paste is synced to it. With the
    // document synced by Rhizome, the paste isn't sent to the backend.
    if (ApplicationInfo.getInstance().build.baselineVersion < 263 || isRhizomeDocumentSyncEnabled()) {
      paste(context)
      return null
    }
    val holder = ActionContinuationHolder()
    paste(SimpleDataContext.getSimpleContext(BACKEND_ACTION_CONTINUATION_KEY, holder, context))
    val continuation = holder.continuation ?: return null
    return { continuation.await() }
  }

  private fun isRhizomeDocumentSyncEnabled(): Boolean = System.getProperty("ijpl.rhizome.ad.enabled").toBoolean()
}
