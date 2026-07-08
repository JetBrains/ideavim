/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.bookmark

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs [block] on EDT.
 *
 * A local copy of `com.maddyhome.idea.vim.group.onEdt`: that one is `internal` to the
 * ideavim-backend module and therefore not visible here. The bookmark RPC handler lives in
 * this module (rather than ideavim-backend) so its classloader has access to the optional
 * `intellij.platform.bookmarks` module.
 */
internal suspend inline fun <T> onEdt(crossinline block: () -> T): T {
  return withContext(Dispatchers.EDT) { block() }
}
