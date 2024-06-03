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
public open class GlobalIjOptions(scope: OptionAccessScope) : OptionsPropertiesBase(scope) {
  public var ide: String by optionProperty(IjOptions.ide)
  public var ideamarks: Boolean by optionProperty(IjOptions.ideamarks)
  public var ideastatusicon: String by optionProperty(IjOptions.ideastatusicon)
  public val ideavimsupport: StringListOptionValue by optionProperty(IjOptions.ideavimsupport)
  public var ideawrite: String by optionProperty(IjOptions.ideawrite)
  public val lookupkeys: StringListOptionValue by optionProperty(IjOptions.lookupkeys)
  public var trackactionids: Boolean by optionProperty(IjOptions.trackactionids)
  public var visualdelay: Int by optionProperty(IjOptions.visualdelay)

  // Temporary options to control work-in-progress behaviour
  public var closenotebooks: Boolean by optionProperty(IjOptions.closenotebooks)
  public var commandOrMotionAnnotation: Boolean by optionProperty(IjOptions.commandOrMotionAnnotation)
  public var oldundo: Boolean by optionProperty(IjOptions.oldundo)
  public var unifyjumps: Boolean by optionProperty(IjOptions.unifyjumps)
  public var vimscriptFunctionAnnotation: Boolean by optionProperty(IjOptions.vimscriptFunctionAnnotation)
}

/**
 * An accessor class for IntelliJ implementation specific option values effective in the given editor
 *
 * As a convenience, this class also provides access to the IntelliJ specific global options, via inheritance.
 */
public class EffectiveIjOptions(scope: OptionAccessScope.EFFECTIVE): GlobalIjOptions(scope) {
  // Vim options that are implemented purely by existing IntelliJ features and not used by vim-engine
  public var breakindent: Boolean by optionProperty(IjOptions.breakindent)
  public val colorcolumn: StringListOptionValue by optionProperty(IjOptions.colorcolumn)
  public var cursorline: Boolean by optionProperty(IjOptions.cursorline)
  public var fileformat: String by optionProperty(IjOptions.fileformat)
  public var list: Boolean by optionProperty(IjOptions.list)
  public var number: Boolean by optionProperty(IjOptions.number)
  public var relativenumber: Boolean by optionProperty(IjOptions.relativenumber)
  public var textwidth: Int by optionProperty(IjOptions.textwidth)
  public var wrap: Boolean by optionProperty(IjOptions.wrap)

  // IntelliJ specific options
  public var ideacopypreprocess: Boolean by optionProperty(IjOptions.ideacopypreprocess)
  public var ideajoin: Boolean by optionProperty(IjOptions.ideajoin)
  public var idearefactormode: String by optionProperty(IjOptions.idearefactormode)
}
