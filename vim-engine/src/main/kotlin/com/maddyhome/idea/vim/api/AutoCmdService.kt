/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.autocmd.AutoCmdEvent

interface AutoCmdService {
  fun handleEvent(event: AutoCmdEvent)
  fun registerEventCommand(command: String, event: AutoCmdEvent)
  fun clearEvents()
}
