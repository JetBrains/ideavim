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
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.StringListOption
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

  public val ide: StringOption = addOption(
    StringOption("ide", GLOBAL, "ide", ApplicationNamesInfo.getInstance().fullProductNameWithEdition)
  )
  public val ideacopypreprocess: ToggleOption = addOption(
    ToggleOption("ideacopypreprocess", GLOBAL_OR_LOCAL_TO_BUFFER, "ideacopypreprocess", false)
  )
  public val ideajoin: ToggleOption = addOption(ToggleOption("ideajoin", GLOBAL_OR_LOCAL_TO_BUFFER, "ideajoin", false))
  public val ideamarks: ToggleOption = addOption(ToggleOption("ideamarks", GLOBAL, "ideamarks", true))
  public val idearefactormode: StringOption = addOption(
    StringOption(
      "idearefactormode",
      GLOBAL_OR_LOCAL_TO_BUFFER,
      "idearefactormode",
      "select",
      IjOptionConstants.ideaRefactorModeValues
    )
  )
  public val ideastatusicon: StringOption = addOption(
    StringOption(
      "ideastatusicon",
      GLOBAL,
      "ideastatusicon",
      "enabled",
      IjOptionConstants.ideaStatusIconValues
    )
  )
  public val ideavimsupport: StringListOption = addOption(
    StringListOption(
      "ideavimsupport",
      GLOBAL,
      "ideavimsupport",
      "dialog",
      IjOptionConstants.ideavimsupportValues
    )
  )
  @JvmField public val ideawrite: StringOption = addOption(
    StringOption("ideawrite", GLOBAL, "ideawrite", "all", IjOptionConstants.ideaWriteValues)
  )
  public val lookupkeys: StringListOption = addOption(
    StringListOption(
      "lookupkeys",
      GLOBAL,
      "lookupkeys",
      "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>")
  )
  public val trackactionids: ToggleOption = addOption(ToggleOption("trackactionids", GLOBAL, "tai", false))
  public val visualdelay: UnsignedNumberOption = addOption(UnsignedNumberOption("visualdelay", GLOBAL, "visualdelay", 100))

  // Temporary feature flags during development, not really intended for external use
  public val closenotebooks: ToggleOption = addOption(ToggleOption("closenotebooks", GLOBAL, "closenotebooks", true, isHidden = true))
  public val commandOrMotionAnnotation: ToggleOption = addOption(ToggleOption("commandormotionannotation", GLOBAL, "commandormotionannotation", true, isHidden = true))
  public val exCommandAnnotation: ToggleOption = addOption(ToggleOption("excommandannotation", GLOBAL, "excommandannotation", true, isHidden = true))
  public val oldundo: ToggleOption = addOption(ToggleOption("oldundo", GLOBAL, "oldundo", false, isHidden = true))
  public val showmodewidget: ToggleOption = addOption(ToggleOption("showmodewidget", GLOBAL, "showmodewidget", false, isHidden = true))
  public val unifyjumps: ToggleOption = addOption(ToggleOption("unifyjumps", GLOBAL, "unifyjumps", true, isHidden = true))
  public val useNewRegex: ToggleOption = addOption(ToggleOption("usenewregex", GLOBAL, "usenewregex", true, isHidden = true))
  public val vimscriptFunctionAnnotation: ToggleOption = addOption(ToggleOption("vimscriptfunctionannotation", GLOBAL, "vimscriptfunctionannotation", true, isHidden = true))

  // This needs to be Option<out VimDataType> so that it can work with derived option types, such as NumberOption, which
  // derives from Option<VimInt>
  private fun <T : Option<out VimDataType>> addOption(option: T) = option.also { Options.addOption(option) }
}
