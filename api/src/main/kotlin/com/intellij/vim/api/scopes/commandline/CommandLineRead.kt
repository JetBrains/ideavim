/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.commandline

import com.intellij.vim.api.scopes.VimApiDsl

/**
 * Scope for command line functions that should be executed under read lock.
 */
@VimApiDsl
interface CommandLineRead {
  /**
   * The text currently displayed in the command line.
   */
  val text: String

  /**
   * The current position of the caret in the command line.
   */
  val caretPosition: Int

  /**
   * True if the command line is currently active, false otherwise.
   */
  val isActive: Boolean
}