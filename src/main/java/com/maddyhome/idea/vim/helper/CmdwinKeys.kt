/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.maddyhome.idea.vim.api.HistoryWindowKind

/** User-data keys placed on the cmdwin's `LightVirtualFile`. */
object CmdwinKeys {
  @JvmField
  val KIND: Key<HistoryWindowKind> = Key.create("ideavim.cmdwin.kind")

  @JvmField
  val ORIGINAL_FILE: Key<VirtualFile> = Key.create("ideavim.cmdwin.originalFile")
}
