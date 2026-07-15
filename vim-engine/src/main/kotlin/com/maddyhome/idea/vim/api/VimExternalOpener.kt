/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Opens a target (a URL or a file path found under the cursor by the `gx` command) with the
 * external program the OS associates with it — a web browser for URLs, the default application for
 * files. This mirrors Vim/Neovim's `gx`, which opens "filepath or URI under cursor with the system
 * handler".
 *
 * The actual launch is platform-specific (e.g. IntelliJ's `BrowserUtil`), so it lives behind this
 * interface: `vim-engine` stays free of any IDE dependency, and tests can substitute a mock (see
 * `MockTestCase.mockService`) to assert which target `gx` resolved without launching anything.
 */
interface VimExternalOpener {
  fun open(target: String)
}
