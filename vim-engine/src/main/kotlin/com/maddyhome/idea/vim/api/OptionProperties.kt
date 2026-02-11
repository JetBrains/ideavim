/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.options.OptionAccessScope

/**
 * An accessor class for global options
 *
 * This class provides access to options that only have a global value, and do not have a separate value local to a
 * buffer (document) or window (editor).
 */
@Suppress("unused", "SpellCheckingInspection")
open class GlobalOptions(scope: OptionAccessScope) : OptionsPropertiesBase(scope) {
  val clipboard: StringListOptionValue by optionProperty(Options.clipboard)
  var digraph: Boolean by optionProperty(Options.digraph)
  var gdefault: Boolean by optionProperty(Options.gdefault)
  val guicursor: StringListOptionValue by optionProperty(Options.guicursor)
  var history: Int by optionProperty(Options.history)
  var hlsearch: Boolean by optionProperty(Options.hlsearch)
  var ignorecase: Boolean by optionProperty(Options.ignorecase)
  var incsearch: Boolean by optionProperty(Options.incsearch)
  val keymodel: StringListOptionValue by optionProperty(Options.keymodel)
  var maxmapdepth: Int by optionProperty(Options.maxmapdepth)
  var more: Boolean by optionProperty(Options.more)
  var operatorfunc: String by optionProperty(Options.operatorfunc)
  var scrolljump: Int by optionProperty(Options.scrolljump)
  var selection: String by optionProperty(Options.selection)
  val selectmode: StringListOptionValue by optionProperty(Options.selectmode)
  var shell: String by optionProperty(Options.shell)
  var shellcmdflag: String by optionProperty(Options.shellcmdflag)
  var shellxescape: String by optionProperty(Options.shellxescape)
  var shellxquote: String by optionProperty(Options.shellxquote)
  var showcmd: Boolean by optionProperty(Options.showcmd)
  var showmode: Boolean by optionProperty(Options.showmode)
  var sidescroll: Int by optionProperty(Options.sidescroll)
  var smartcase: Boolean by optionProperty(Options.smartcase)
  var startofline: Boolean by optionProperty(Options.startofline)
  val timeout: Boolean by optionProperty(Options.timeout)
  var timeoutlen: Int by optionProperty(Options.timeoutlen)
  val viminfo: StringListOptionValue by optionProperty(Options.viminfo)
  var visualbell: Boolean by optionProperty(Options.visualbell)
  val whichwrap: StringListOptionValue by optionProperty(Options.whichwrap)
  var wrapscan: Boolean by optionProperty(Options.wrapscan)

  // IdeaVim specific options. Put any editor or IDE specific options in IjOptionProperties
  var maxhlduringincsearch: Int by optionProperty(Options.maxhlduringincsearch)
  var showmatchcount: Boolean by optionProperty(Options.showmatchcount)

  // Temporary flags for work-in-progress behaviour. Hidden from the output of `:set all`
  var ideastrictmode: Boolean by optionProperty(Options.ideastrictmode)
  var ideatracetime: Boolean by optionProperty(Options.ideatracetime)
}

/**
 * An accessor class for the values of options in effect in the given editor
 *
 * As a convenience, this class also provides access to the global options, via inheritance.
 */
@Suppress("unused")
open class EffectiveOptions(scope: OptionAccessScope.EFFECTIVE) : GlobalOptions(scope) {
  val iskeyword: StringListOptionValue by optionProperty(Options.iskeyword)
  val matchpairs: StringListOptionValue by optionProperty(Options.matchpairs)
  val nrformats: StringListOptionValue by optionProperty(Options.nrformats)
  var number: Boolean by optionProperty(Options.number)
  var scroll: Int by optionProperty(Options.scroll)
  var scrolloff: Int by optionProperty(Options.scrolloff)
  var sidescrolloff: Int by optionProperty(Options.sidescrolloff)
  var undolevels: Int by optionProperty(Options.undolevels)
  val virtualedit: StringListOptionValue by optionProperty(Options.virtualedit)
}
