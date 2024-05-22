/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimCommandLineService {
  public fun getActiveCommandLine(): VimCommandLine?

  /**
   * Turns on the command line for the given editor
   *
   * @param editor   The editor to use for display
   * @param context  The data context
   * @param label    The label for the command line (i.e. :, /, or ?)
   * @param initText The initial text for the entry
   * @param count    TODO
   */
  public fun create(editor: VimEditor, context: ExecutionContext, label: String, initText: String, count: Int): VimCommandLine
}