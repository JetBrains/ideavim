/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor

/**
 * Get caret line in vim notation (1-based)
 */
internal val Caret.vimLine: Int
  get() = this.logicalPosition.line + 1

/**
 * Get current caret line in vim notation (1-based)
 */
internal val Editor.vimLine: Int
  get() = this.caretModel.currentCaret.vimLine
