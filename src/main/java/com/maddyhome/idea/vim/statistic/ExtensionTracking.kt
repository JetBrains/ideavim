/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.statistic

import java.util.concurrent.ConcurrentHashMap

/**
 * Shared extension tracking data used by both the statistics collectors (in frontend)
 * and the extension registrar (in common).
 */
object ExtensionTracking {
  val extensionNames = listOf(
    "textobj-entire",
    "argtextobj",
    "ReplaceWithRegister",
    "vim-paragraph-motion",
    "highlightedyank",
    "multiple-cursors",
    "exchange",
    "NERDTree",
    "surround",
    "commentary",
    "matchit",
    "textobj-indent",
    "mini-ai"
  )
  val enabledExtensions: MutableSet<String> = ConcurrentHashMap.newKeySet()
}
