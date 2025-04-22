/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.OptionsPropertiesBase
import com.maddyhome.idea.vim.api.StringListOptionValue
import com.maddyhome.idea.vim.options.OptionAccessScope

/**
 * An accessor class for IntelliJ implementation specific global options
 *
 * This class will only access IntelliJ specific global options. It does not provide access to Vim standard global
 * options
 */
@Suppress("SpellCheckingInspection")
open class GlobalIjOptions(scope: OptionAccessScope) : OptionsPropertiesBase(scope) {
  var ide: String by optionProperty(IjOptions.ide)
  var ideamarks: Boolean by optionProperty(IjOptions.ideamarks)
  var ideastatusicon: String by optionProperty(IjOptions.ideastatusicon)
  val ideavimsupport: StringListOptionValue by optionProperty(IjOptions.ideavimsupport)
  var ideawrite: String by optionProperty(IjOptions.ideawrite)
  val lookupkeys: StringListOptionValue by optionProperty(IjOptions.lookupkeys)
  var trackactionids: Boolean by optionProperty(IjOptions.trackactionids)
  var visualdelay: Int by optionProperty(IjOptions.visualdelay)

  // Temporary options to control work-in-progress behaviour
  var closenotebooks: Boolean by optionProperty(IjOptions.closenotebooks)
  var oldundo: Boolean by optionProperty(IjOptions.oldundo)
  var unifyjumps: Boolean by optionProperty(IjOptions.unifyjumps)
}

/**
 * An accessor class for IntelliJ implementation specific option values effective in the given editor
 *
 * As a convenience, this class also provides access to the IntelliJ specific global options, via inheritance.
 */
class EffectiveIjOptions(scope: OptionAccessScope.EFFECTIVE) : GlobalIjOptions(scope) {
  // Vim options that are implemented purely by existing IntelliJ features and not used by vim-engine
  var breakindent: Boolean by optionProperty(IjOptions.breakindent)
  val colorcolumn: StringListOptionValue by optionProperty(IjOptions.colorcolumn)
  var cursorline: Boolean by optionProperty(IjOptions.cursorline)
  var fileformat: String by optionProperty(IjOptions.fileformat)
  var list: Boolean by optionProperty(IjOptions.list)
  var relativenumber: Boolean by optionProperty(IjOptions.relativenumber)
  var textwidth: Int by optionProperty(IjOptions.textwidth)
  var wrap: Boolean by optionProperty(IjOptions.wrap)

  // IntelliJ specific options
  var ideacopypreprocess: Boolean by optionProperty(IjOptions.ideacopypreprocess)
  var ideajoin: Boolean by optionProperty(IjOptions.ideajoin)
  var idearefactormode: String by optionProperty(IjOptions.idearefactormode)
}
