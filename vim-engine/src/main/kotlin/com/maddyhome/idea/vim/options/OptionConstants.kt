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
    const val clipboard = "clipboard"
    const val clipboardAlias = "cb"
    const val clipboard_ideaput = "ideaput"
    const val clipboard_unnamed = "unnamed"

    const val digraph = "digraph"
    const val gdefault = "gdefault"
    const val guicursor = "guicursor"
    const val history = "history"
    const val hlsearch = "hlsearch"
    const val ignorecase = "ignorecase"
    const val incsearch = "incsearch"
    const val iskeyword = "iskeyword"

    const val keymodel = "keymodel"
    const val keymodel_startsel = "startsel"
    const val keymodel_stopsel = "stopsel"
    const val keymodel_stopselect = "stopselect"
    const val keymodel_stopvisual = "stopvisual"
    const val keymodel_continueselect = "continueselect"
    const val keymodel_continuevisual = "continuevisual"

    const val matchpairs = "matchpairs"
    const val maxmapdepth = "maxmapdepth"
    const val more = "more"
    const val nrformats = "nrformats"
    const val number = "number"
    const val relativenumber = "relativenumber"
    const val scroll = "scroll"
    const val scrolljump = "scrolljump"
    const val scrolloff = "scrolloff"
    const val selection = "selection"

    const val selectmode = "selectmode"
    const val selectmode_mouse = "mouse"
    const val selectmode_key = "key"
    const val selectmode_cmd = "cmd"
    const val selectmode_ideaselection = "ideaselection"

    const val shell = "shell"
    const val shellcmdflag = "shellcmdflag"
    const val shellxescape = "shellxescape"
    const val shellxquote = "shellxquote"
    const val showcmd = "showcmd"
    const val showmode = "showmode"
    const val sidescroll = "sidescroll"
    const val sidescrolloff = "sidescrolloff"
    const val smartcase = "smartcase"
    const val startofline = "startofline"
    const val timeout = "timeout"
    const val timeoutlen = "timeoutlen"
    const val undolevels = "undolevels"
    const val visualbell = "visualbell"
    const val wrapscan = "wrapscan"
    const val whichwrap = "whichwrap"
    const val viminfo = "viminfo"

    const val virtualedit = "virtualedit"
    const val virtualedit_onemore = "onemore"
    const val virtualedit_block = "block"
    const val virtualedit_insert = "insert"
    const val virtualedit_all = "all"

    // IdeaVim specific options
    const val ideaglobalmode = "ideaglobalmode"
    const val ideastrictmode = "ideastrictmode"
    const val ideatracetime = "ideatracetime"
    const val octopushandler = "octopushandler"
  }
}
