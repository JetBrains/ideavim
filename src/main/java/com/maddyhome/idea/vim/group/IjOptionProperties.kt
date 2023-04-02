/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.GlobalOptions
import com.maddyhome.idea.vim.api.StringListOptionValue
import com.maddyhome.idea.vim.options.OptionScope

/**
 * An accessor class for IntelliJ implementation specific global options
 *
 * This class will only access IntelliJ specific global options. It does not provide access to Vim standard global
 * options
 */
@Suppress("SpellCheckingInspection")
public open class GlobalIjOptions(scope: OptionScope = OptionScope.GLOBAL) : GlobalOptions(scope) {
  public var closenotebooks: Boolean by optionProperty(IjOptions.closenotebooks)
  public var ide: String by optionProperty(IjOptions.ide)
  public var ideamarks: Boolean by optionProperty(IjOptions.ideamarks)
  public var ideastatusicon: String by optionProperty(IjOptions.ideastatusicon)
  public val ideavimsupport: StringListOptionValue by optionProperty(IjOptions.ideavimsupport)
  public var ideawrite: String by optionProperty(IjOptions.ideawrite)
  public val lookupkeys: StringListOptionValue by optionProperty(IjOptions.lookupkeys)
  public var trackactionids: Boolean by optionProperty(IjOptions.trackactionids)
  public var visualdelay: Int by optionProperty(IjOptions.visualdelay)

  // Temporary options to control work-in-progress behaviour
  public var octopushandler: Boolean by optionProperty(IjOptions.octopushandler)
  public var oldundo: Boolean by optionProperty(IjOptions.oldundo)
  public var unifyjumps: Boolean by optionProperty(IjOptions.unifyjumps)
}

/**
 * An accessor class for IntelliJ implementation specific option values effective for the given local scope
 *
 * As a convenience, this class will also provide access to the global options that are effective for the local scope.
 */
public class EffectiveIjOptions(scope: OptionScope.LOCAL): GlobalIjOptions(scope) {
  public var ideacopypreprocess: Boolean by optionProperty(IjOptions.ideacopypreprocess)
  public var ideajoin: Boolean by optionProperty(IjOptions.ideajoin)
  public var idearefactormode: String by optionProperty(IjOptions.idearefactormode)
}
