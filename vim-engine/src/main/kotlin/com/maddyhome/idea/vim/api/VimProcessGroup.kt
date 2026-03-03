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

  /**
   * Executes a shell command.
   *
   * Shell options ([GlobalOptions.shell], [GlobalOptions.shellcmdflag], etc.) must be provided
   * by the caller because in split (Remote Development) mode the backend does not have access
   * to the user's Vim option values — they live on the frontend.
   *
   * @param options global Vim options containing shell configuration; callers should pass
   *   `injector.globalOptions()` which reads from the frontend's option storage
   */
  @kotlin.jvm.Throws(java.lang.Exception::class)
  fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?,
    options: GlobalOptions,
  ): String?
}
