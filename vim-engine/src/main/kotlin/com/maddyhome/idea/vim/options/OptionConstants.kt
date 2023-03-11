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
    public const val clipboard: String = "clipboard"
    public const val clipboardAlias: String = "cb"
    public const val clipboard_ideaput: String = "ideaput"
    public const val clipboard_unnamed: String = "unnamed"

    public const val digraph: String = "digraph"
    public const val gdefault: String = "gdefault"
    public const val guicursor: String = "guicursor"
    public const val history: String = "history"
    public const val hlsearch: String = "hlsearch"
    public const val ignorecase: String = "ignorecase"
    public const val incsearch: String = "incsearch"
    public const val iskeyword: String = "iskeyword"

    public const val keymodel: String = "keymodel"
    public const val keymodel_startsel: String = "startsel"
    public const val keymodel_stopsel: String = "stopsel"
    public const val keymodel_stopselect: String = "stopselect"
    public const val keymodel_stopvisual: String = "stopvisual"
    public const val keymodel_continueselect: String = "continueselect"
    public const val keymodel_continuevisual: String = "continuevisual"

    public const val matchpairs: String = "matchpairs"
    public const val maxmapdepth: String = "maxmapdepth"
    public const val more: String = "more"
    public const val nrformats: String = "nrformats"
    public const val number: String = "number"
    public const val relativenumber: String = "relativenumber"
    public const val scroll: String = "scroll"
    public const val scrolljump: String = "scrolljump"
    public const val scrolloff: String = "scrolloff"
    public const val selection: String = "selection"

    public const val selectmode: String = "selectmode"
    public const val selectmode_mouse: String = "mouse"
    public const val selectmode_key: String = "key"
    public const val selectmode_cmd: String = "cmd"
    public const val selectmode_ideaselection: String = "ideaselection"

    public const val shell: String = "shell"
    public const val shellcmdflag: String = "shellcmdflag"
    public const val shellxescape: String = "shellxescape"
    public const val shellxquote: String = "shellxquote"
    public const val showcmd: String = "showcmd"
    public const val showmode: String = "showmode"
    public const val sidescroll: String = "sidescroll"
    public const val sidescrolloff: String = "sidescrolloff"
    public const val smartcase: String = "smartcase"
    public const val startofline: String = "startofline"
    public const val timeout: String = "timeout"
    public const val timeoutlen: String = "timeoutlen"
    public const val undolevels: String = "undolevels"
    public const val visualbell: String = "visualbell"
    public const val wrapscan: String = "wrapscan"
    public const val whichwrap: String = "whichwrap"
    public const val viminfo: String = "viminfo"

    public const val virtualedit: String = "virtualedit"
    public const val virtualedit_onemore: String = "onemore"
    public const val virtualedit_block: String = "block"
    public const val virtualedit_insert: String = "insert"
    public const val virtualedit_all: String = "all"

    // IdeaVim specific options
    public const val ideaglobalmode: String = "ideaglobalmode"
    public const val ideastrictmode: String = "ideastrictmode"
    public const val ideatracetime: String = "ideatracetime"
  }
}
