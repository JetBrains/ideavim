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
  public val label: String
  public var text: String
  public val caret: VimCommandLineCaret

  public fun handleKey(key: KeyStroke)
  public fun deactivate(refocusOwningEditor: Boolean, resetCaret: Boolean)

  public fun setCurrentActionPromptCharacter(char: Char)
}