/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import javax.swing.KeyStroke

public interface VimCommandLine {
  public val caret: VimCommandLineCaret

  public val label: String

  /**
   * The actual text present in the command line, excluding special characters like the `?` displayed during digraph input.
   * This text represents the real content that is being processed or executed.
   */
  public val actualText: String

  /**
   * The text content displayed in the command line, including any additional characters or symbols
   * that might be shown to the user, such as the `?` during digraph input.
   * This is the text that the user sees on the screen.
   */
  public val visibleText: String

  public fun setText(string: String)
  public fun handleKey(key: KeyStroke)
  public fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean)

  public fun setPromptCharacter(char: Char)
  public fun clearPromptCharacter()

  public fun clearCurrentAction()
}