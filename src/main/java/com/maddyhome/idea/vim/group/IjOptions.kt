/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationNamesInfo
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

@Suppress("SpellCheckingInspection")
public object IjOptions {

  public fun initialise() {
    // Calling this method allows for deterministic initialisation of IjOptions, specifically initialising the
    // properties and registering the IJ specific options. Once added, they can be safely accessed by name, e.g. by the
    // implementation of `:set` while executing ~/.ideavimrc

    // We also override the default value of 'clipboard', because IntelliJ supports the 'ideaput' item, which will use
    // IntelliJ's paste processes (e.g. to convert pasted Java to Kotlin)
    Options.overrideDefaultValue(Options.clipboard, VimString("ideaput,autoselect,exclude:cons\\|linux"))
  }

  public val closenotebooks: ToggleOption = addOption(ToggleOption("closenotebooks", "closenotebooks", true))
  public val ide: StringOption =
    addOption(StringOption("ide", "ide", ApplicationNamesInfo.getInstance().fullProductNameWithEdition))
  public val ideacopypreprocess: ToggleOption =
    addOption(ToggleOption("ideacopypreprocess", "ideacopypreprocess", false))
  public val ideajoin: ToggleOption = addOption(ToggleOption("ideajoin", "ideajoin", false))
  public val ideamarks: ToggleOption = addOption(ToggleOption("ideamarks", "ideamarks", true))
  public val idearefactormode: StringOption = addOption(
    StringOption(
      "idearefactormode",
      "idearefactormode",
      "select",
      isList = false,
      IjOptionConstants.ideaRefactorModeValues
    )
  )
  public val ideastatusicon: StringOption = addOption(
    StringOption(
      "ideastatusicon",
      "ideastatusicon",
      "enabled",
      isList = false,
      IjOptionConstants.ideaStatusIconValues
    )
  )
  public val ideavimsupport: StringOption = addOption(
    StringOption(
      "ideavimsupport",
      "ideavimsupport",
      "dialog",
      isList = true,
      IjOptionConstants.ideavimsupportValues
    )
  )
  @JvmField public val ideawrite: StringOption =
    addOption(StringOption("ideawrite", "ideawrite", "all", isList = false, IjOptionConstants.ideaWriteValues))
  public val lookupkeys: StringOption = addOption(
    StringOption(
      "lookupkeys",
      "lookupkeys",
      "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>",
      isList = true
    )
  )
  public val octopushandler: ToggleOption = addOption(ToggleOption("octopushandler", "octopushandler", false))
  public val oldundo: ToggleOption = addOption(ToggleOption("oldundo", "oldundo", true))
  public val trackactionids: ToggleOption = addOption(ToggleOption("trackactionids", "tai", false))
  public val unifyjumps: ToggleOption = addOption(ToggleOption("unifyjumps", "unifyjumps", true))
  public val visualdelay: UnsignedNumberOption = addOption(UnsignedNumberOption("visualdelay", "visualdelay", 100))

  // This needs to be Option<out VimDataType> so that it can work with derived option types, such as NumberOption, which
  // derives from Option<VimInt>
  private fun <T : Option<out VimDataType>> addOption(option: T) = option.also { Options.addOption(option) }
}