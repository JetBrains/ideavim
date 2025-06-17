/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api


sealed interface Mode {
  val returnTo: Mode

  data class NORMAL(private val originalMode: Mode? = null) : Mode {
    override val returnTo: Mode
      get() = originalMode ?: this
  }

  data class OP_PENDING(override val returnTo: Mode) : Mode

  data class VISUAL(val selectionType: TextType, override val returnTo: Mode = NORMAL()) : Mode

  data class SELECT(val selectionType: TextType, override val returnTo: Mode = NORMAL()) : Mode

  object INSERT : Mode {
    override val returnTo: Mode = NORMAL()
  }

  object REPLACE : Mode {
    override val returnTo: Mode = NORMAL()
  }

  data class CMD_LINE(override val returnTo: Mode) : Mode
}