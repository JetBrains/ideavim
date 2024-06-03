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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL
import com.maddyhome.idea.vim.options.OptionDeclaredScope.GLOBAL_OR_LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_BUFFER
import com.maddyhome.idea.vim.options.OptionDeclaredScope.LOCAL_TO_WINDOW
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

  // Vim options that are implemented purely by existing IntelliJ features and not used by vim-engine
  public val breakindent: ToggleOption = addOption(ToggleOption("breakindent", LOCAL_TO_WINDOW, "bri", false))
  public val colorcolumn: StringListOption = addOption(object : StringListOption("colorcolumn", LOCAL_TO_WINDOW, "cc", "") {
    override fun checkIfValueValid(value: VimDataType, token: String) {
      super.checkIfValueValid(value, token)
      if (value != VimString.EMPTY) {
        // Each element in the comma-separated string list needs to be a number. No spaces. Vim supports numbers
        // beginning "+" or "-" to draw a highlight column relative to the 'textwidth' value. We don't fully support
        // that, but we do automatically add "+0" because IntelliJ always displays the right margin
        split((value as VimString).asString()).forEach {
          if (!it.matches(Regex("[+-]?[0-9]+"))) {
            throw exExceptionMessage("E474", token)
          }
        }
      }
    }
  })
  public val cursorline: ToggleOption = addOption(ToggleOption("cursorline", LOCAL_TO_WINDOW, "cul", false))
  public val list: ToggleOption = addOption(ToggleOption("list", LOCAL_TO_WINDOW, "list", false))
  public val number: ToggleOption = addOption(ToggleOption("number", LOCAL_TO_WINDOW, "nu", false))
  public val relativenumber: ToggleOption = addOption(ToggleOption("relativenumber", LOCAL_TO_WINDOW, "rnu", false))
  public val textwidth: NumberOption = addOption(UnsignedNumberOption("textwidth", LOCAL_TO_BUFFER, "tw", 0))
  public val wrap: ToggleOption = addOption(ToggleOption("wrap", LOCAL_TO_WINDOW, "wrap", true))

  // These options are not explicitly listed as local-noglobal in Vim's help, but are set when a new buffer is edited,
  // based on the value of 'fileformats' or 'fileencodings'. To prevent unexpected file cnversion, we treat them as
  // local-noglobal. See `:help local-noglobal`, `:help 'fileformats'` and `:help 'fileencodings'`
  public val bomb: ToggleOption =
    addOption(ToggleOption("bomb", LOCAL_TO_BUFFER, "bomb", false, isLocalNoGlobal = true))
  public val fileencoding: StringOption = addOption(
    StringOption(
      "fileencoding",
      LOCAL_TO_BUFFER,
      "fenc",
      VimString.EMPTY,
      isLocalNoGlobal = true
    )
  )
  public val fileformat: StringOption = addOption(
    StringOption(
      "fileformat",
      LOCAL_TO_BUFFER,
      "ff",
      if (injector.systemInfoService.isWindows) "dos" else "unix",
      boundedValues = setOf("dos", "unix", "mac"),
      isLocalNoGlobal = true
    )
  )

  // IntelliJ specific functionality - custom options
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
  public val oldundo: ToggleOption = addOption(ToggleOption("oldundo", GLOBAL, "oldundo", false, isHidden = true))
  public val unifyjumps: ToggleOption = addOption(ToggleOption("unifyjumps", GLOBAL, "unifyjumps", true, isHidden = true))
  public val vimscriptFunctionAnnotation: ToggleOption = addOption(ToggleOption("vimscriptfunctionannotation", GLOBAL, "vimscriptfunctionannotation", true, isHidden = true))

  // This needs to be Option<out VimDataType> so that it can work with derived option types, such as NumberOption, which
  // derives from Option<VimInt>
  private fun <T : Option<out VimDataType>> addOption(option: T) = option.also { Options.addOption(option) }
}
