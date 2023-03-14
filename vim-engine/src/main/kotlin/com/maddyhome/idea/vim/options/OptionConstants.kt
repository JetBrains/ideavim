/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

public class OptionConstants {
  public companion object {
    public const val clipboard_ideaput: String = "ideaput"
    public const val clipboard_unnamed: String = "unnamed"

    public const val keymodel_startsel: String = "startsel"
    public const val keymodel_stopsel: String = "stopsel"
    public const val keymodel_stopselect: String = "stopselect"
    public const val keymodel_stopvisual: String = "stopvisual"
    public const val keymodel_continueselect: String = "continueselect"
    public const val keymodel_continuevisual: String = "continuevisual"

    public const val selectmode_mouse: String = "mouse"
    public const val selectmode_key: String = "key"
    public const val selectmode_cmd: String = "cmd"
    public const val selectmode_ideaselection: String = "ideaselection"

    public const val virtualedit_onemore: String = "onemore"
    public const val virtualedit_block: String = "block"
    public const val virtualedit_insert: String = "insert"
    public const val virtualedit_all: String = "all"
  }
}
