/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

interface VimProcessGroup {
  /**
   * Exit code from the last [executeCommand] call.
   * Callers can check this after execution to display "shell returned X" messages.
   */
  val lastExitCode: Int?

  @kotlin.jvm.Throws(java.lang.Exception::class)
  fun executeCommand(editor: VimEditor, command: String, input: CharSequence?, currentDirectoryPath: String?): String?
}
