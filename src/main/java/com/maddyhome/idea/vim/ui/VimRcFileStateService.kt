/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.maddyhome.idea.vim.api.VimrcFileState

/**
 * Service wrapper that delegates to the VimRcFileState singleton.
 * Registered with serviceInterface=VimrcFileState so that IjVimInjector can resolve it
 * via `service()` without directly importing VimRcFileState.
 */
internal class VimRcFileStateService : VimrcFileState by VimRcFileState
