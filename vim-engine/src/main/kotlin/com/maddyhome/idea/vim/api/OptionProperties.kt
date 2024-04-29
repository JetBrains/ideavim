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
public open class GlobalOptions(scope: OptionAccessScope): OptionsPropertiesBase(scope) {
  public val clipboard: StringListOptionValue by optionProperty(Options.clipboard)
  public var digraph: Boolean by optionProperty(Options.digraph)
  public var gdefault: Boolean by optionProperty(Options.gdefault)
  public val guicursor: StringListOptionValue by optionProperty(Options.guicursor)
  public var history: Int by optionProperty(Options.history)
  public var hlsearch: Boolean by optionProperty(Options.hlsearch)
  public var ignorecase: Boolean by optionProperty(Options.ignorecase)
  public var incsearch: Boolean by optionProperty(Options.incsearch)
  public val keymodel: StringListOptionValue by optionProperty(Options.keymodel)
  public var maxmapdepth: Int by optionProperty(Options.maxmapdepth)
  public var more: Boolean by optionProperty(Options.more)
  public var operatorfunc: String by optionProperty(Options.operatorfunc)
  public var scrolljump: Int by optionProperty(Options.scrolljump)
  public var selection: String by optionProperty(Options.selection)
  public val selectmode: StringListOptionValue by optionProperty(Options.selectmode)
  public var shell: String by optionProperty(Options.shell)
  public var shellcmdflag: String by optionProperty(Options.shellcmdflag)
  public var shellxescape: String by optionProperty(Options.shellxescape)
  public var shellxquote: String by optionProperty(Options.shellxquote)
  public var showcmd: Boolean by optionProperty(Options.showcmd)
  public var showmode: Boolean by optionProperty(Options.showmode)
  public var sidescroll: Int by optionProperty(Options.sidescroll)
  public var smartcase: Boolean by optionProperty(Options.smartcase)
  public var startofline: Boolean by optionProperty(Options.startofline)
  public val timeout: Boolean by optionProperty(Options.timeout)
  public var timeoutlen: Int by optionProperty(Options.timeoutlen)
  public val viminfo: StringListOptionValue by optionProperty(Options.viminfo)
  public var visualbell: Boolean by optionProperty(Options.visualbell)
  public val whichwrap: StringListOptionValue by optionProperty(Options.whichwrap)
  public var wrapscan: Boolean by optionProperty(Options.wrapscan)

  // IdeaVim specific options. Put any editor or IDE specific options in IjOptionProperties

  // This is an experimental option that enables global mode for the editor. However,
  //   for the moment it has issues and there is no quality garantee if this option is enabled
  public var ideaglobalmode: Boolean by optionProperty(Options.ideaglobalmode)

  // Temporary flags for work-in-progress behaviour. Hidden from the output of `:set all`
  public var ideastrictmode: Boolean by optionProperty(Options.ideastrictmode)
  public var ideatracetime: Boolean by optionProperty(Options.ideatracetime)
}

/**
 * An accessor class for the values of options in effect in the given editor
 *
 * As a convenience, this class also provides access to the global options, via inheritance.
 */
@Suppress("unused")
public open class EffectiveOptions(scope: OptionAccessScope.EFFECTIVE): GlobalOptions(scope) {
  public val iskeyword: StringListOptionValue by optionProperty(Options.iskeyword)
  public val matchpairs: StringListOptionValue by optionProperty(Options.matchpairs)
  public val nrformats: StringListOptionValue by optionProperty(Options.nrformats)
  public var scroll: Int by optionProperty(Options.scroll)
  public var scrolloff: Int by optionProperty(Options.scrolloff)
  public var sidescrolloff: Int by optionProperty(Options.sidescrolloff)
  public var undolevels: Int by optionProperty(Options.undolevels)
  public val virtualedit: StringListOptionValue by optionProperty(Options.virtualedit)
}
