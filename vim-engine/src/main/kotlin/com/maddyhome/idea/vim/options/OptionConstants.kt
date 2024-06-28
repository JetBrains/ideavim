/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

class OptionConstants {
  companion object {
    const val clipboard_ideaput: String = "ideaput"
    const val clipboard_unnamed: String = "unnamed"
    const val clipboard_unnamedplus: String = "unnamedplus"

    const val keymodel_startsel: String = "startsel"
    const val keymodel_stopsel: String = "stopsel"
    const val keymodel_stopselect: String = "stopselect"
    const val keymodel_stopvisual: String = "stopvisual"
    const val keymodel_continueselect: String = "continueselect"
    const val keymodel_continuevisual: String = "continuevisual"

    const val selectmode_mouse: String = "mouse"
    const val selectmode_key: String = "key"
    const val selectmode_cmd: String = "cmd"
    const val selectmode_ideaselection: String = "ideaselection"

    const val virtualedit_onemore: String = "onemore"
    const val virtualedit_block: String = "block"
    const val virtualedit_insert: String = "insert"
    const val virtualedit_all: String = "all"
  }
}
