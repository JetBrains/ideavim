/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

// Option names for use during testing. We want to encourage the use of strongly typed accessors in production code,
// and these are only used in test infrastructure, so declare them here
class TestOptionConstants {
  @Suppress("SpellCheckingInspection")
  companion object {
    const val clipboard = "clipboard"
    const val guicursor = "guicursor"
    const val ignorecase = "ignorecase"
    const val keymodel = "keymodel"
    const val maxmapdepth = "maxmapdepth"
    const val nrformats = "nrformats"
    const val number = "number"
    const val relativenumber = "relativenumber"
    const val scrolljump = "scrolljump"
    const val scrolloff = "scrolloff"
    const val selectmode = "selectmode"
    const val showmode = "showmode"
    const val sidescroll = "sidescroll"
    const val sidescrolloff = "sidescrolloff"
    const val smartcase = "smartcase"
    const val timeoutlen = "timeoutlen"
    const val virtualedit = "virtualedit"
    const val whichwrap = "whichwrap"

    // IdeaVim specific
    const val ideatracetime = "ideatracetime"
  }
}

class TestIjOptionConstants {
  @Suppress("SpellCheckingInspection")
  companion object {

    // IntelliJ implementation specific
    const val ideajoin = "ideajoin"
    const val ideamarks = "ideamarks"
    const val idearefactormode = "idearefactormode"
    const val ideavimsupport = "ideavimsupport"
    const val trackactionids = "trackactionids"
    const val unifyjumps = "unifyjumps"
  }
}
