/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimCommandLineService {
  fun isCommandLineSupported(editor: VimEditor): Boolean

  fun getActiveCommandLine(): VimCommandLine?

  fun readInputAndProcess(
    vimEditor: VimEditor,
    context: ExecutionContext,
    prompt: String,
    finishOn: Char?,
    processing: (String) -> Unit,
  )

  /**
   * Turns on the command line for the given editor
   *
   * @param editor   The editor to use for display
   * @param context  The data context
   * @param label    The label for the command line (i.e. :, /, or ?)
   * @param initialText The initial text for the entry
   */
  fun createSearchPrompt(
    editor: VimEditor,
    context: ExecutionContext,
    label: String,
    initialText: String,
  ): VimCommandLine

  fun createCommandPrompt(
    editor: VimEditor,
    context: ExecutionContext,
    count0: Int,
    initialText: String,
  ): VimCommandLine

  fun fullReset()
}
