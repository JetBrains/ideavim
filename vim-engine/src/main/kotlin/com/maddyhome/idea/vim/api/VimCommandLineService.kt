/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction

interface VimCommandLineService {
  fun isCommandLineSupported(editor: VimEditor): Boolean

  fun getActiveCommandLine(): VimCommandLine?

  /**
   * Reads a line of text and passes it to [processing]
   *
   * The type of the command line is guessed from [prompt], see [CommandLineType.fromLabel]. Prefer the overload that
   * takes the type.
   */
  fun readInputAndProcess(
    vimEditor: VimEditor,
    context: ExecutionContext,
    prompt: String,
    finishOn: Char?,
    processing: (String) -> Unit,
  ) {
    readInputAndProcess(vimEditor, context, CommandLineType.fromLabel(prompt), prompt, finishOn, processing)
  }

  /**
   * Reads a line of text and passes it to [processing]
   *
   * @param type     The kind of command line, e.g. [CommandLineType.Input] or [CommandLineType.Expression]
   * @param prompt   The label shown before the text
   * @param finishOn A character that finishes the input in addition to `<CR>`
   */
  fun readInputAndProcess(
    vimEditor: VimEditor,
    context: ExecutionContext,
    type: CommandLineType,
    prompt: String,
    finishOn: Char?,
    processing: (String) -> Unit,
  )

  /**
   * Turns on the command line for the given editor
   *
   * The type of the command line is guessed from [label], see [CommandLineType.fromLabel]. Prefer the overload that
   * takes the search direction, or [createInputPrompt] for an input prompt.
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

  /**
   * Turns on the command line for a search pattern, labelled `/` for [Direction.FORWARDS] and `?` for
   * [Direction.BACKWARDS]
   */
  fun createSearchPrompt(
    editor: VimEditor,
    context: ExecutionContext,
    direction: Direction,
    initialText: String,
  ): VimCommandLine

  /**
   * Turns on the command line for an input prompt, such as `input()`. The [prompt] is only shown as the label
   */
  fun createInputPrompt(
    editor: VimEditor,
    context: ExecutionContext,
    prompt: String,
  ): VimCommandLine

  fun createCommandPrompt(
    editor: VimEditor,
    context: ExecutionContext,
    count0: Int,
    initialText: String,
  ): VimCommandLine

  /**
   * Collects a string from the user via a command-line prompt, blocking until input is complete.
   * Used by extension API (input() function). Returns null if the user cancels.
   */
  @Deprecated("Please use readInputAndProcess")
  fun inputString(editor: VimEditor, context: ExecutionContext, prompt: String, finishOn: Char?): String? {
    return null
  }

  fun fullReset()

  /**
   * Returns the pixel height of the active command line (e.g., the ex entry panel), or 0 if no command line is active.
   * This is used by scroll calculations to account for the command line reducing the visible editor area.
   */
  fun getActiveCommandLineHeight(): Int?
}
