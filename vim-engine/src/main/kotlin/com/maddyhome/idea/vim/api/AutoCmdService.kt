/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.autocmd.AutoCmdEvent

/**
 * Registry for Vim-style `:autocmd` event handlers.
 *
 * Handlers are registered per [AutoCmdEvent] with a file pattern and a Vimscript command to run. When an event is
 * fired via [handleEvent], every registered command whose pattern matches the file path is executed against the
 * current editor.
 *
 * Handlers can be grouped via [startAugroup]/[endAugroup]; [clearAugroup] removes all handlers in a group and
 * [clearEvents] clears every handler (or just the active group, when one is open).
 */
interface AutoCmdService {

  /**
   * Fires [event] and runs any registered handlers whose pattern matches [filePath].
   *
   * [filePath] is a VFS path as returned by `VirtualFile.path` — not a native filesystem path — which is why it is a
   * [String] rather than `java.nio.file.Path`. Vim pattern matching is textual and treats the value opaquely.
   */
  fun handleEvent(event: AutoCmdEvent, filePath: String? = null, editor: VimEditor? = null)

  /** Registers [command] to run for [event] when the file path matches [pattern] (default: all files). */
  fun registerEventCommand(command: String, event: AutoCmdEvent, pattern: String = "*")

  /** Removes every registered handler. If an augroup is active ([startAugroup]), only that group is cleared. */
  fun clearEvents()

  /** Opens an augroup named [name]; subsequent [registerEventCommand] calls tag handlers with this group. */
  fun startAugroup(name: String)

  /** Closes the currently open augroup. Has no effect if no group is open. */
  fun endAugroup()

  /** Removes every handler registered under the augroup [name]. */
  fun clearAugroup(name: String)
}
