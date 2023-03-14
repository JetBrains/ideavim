package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationNamesInfo
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public object IjOptions {
  public fun initialise() {
    addOption(ToggleOption(IjOptionConstants.closenotebooks, IjOptionConstants.closenotebooks, true))
    addOption(
      StringOption(
        IjOptionConstants.ide,
        IjOptionConstants.ide,
        ApplicationNamesInfo.getInstance().fullProductNameWithEdition
      )
    )
    addOption(ToggleOption(IjOptionConstants.ideacopypreprocess, IjOptionConstants.ideacopypreprocess, false))
    addOption(ToggleOption(IjOptionConstants.ideajoin, IjOptionConstants.ideajoin, false))
    addOption(ToggleOption(IjOptionConstants.ideamarks, IjOptionConstants.ideamarks, true))
    addOption(
      StringOption(
        IjOptionConstants.idearefactormode,
        IjOptionConstants.idearefactormode,
        "select",
        isList = false,
        IjOptionConstants.ideaRefactorModeValues
      )
    )
    addOption(
      StringOption(
        IjOptionConstants.ideastatusicon,
        IjOptionConstants.ideastatusicon,
        "enabled",
        isList = false,
        IjOptionConstants.ideaStatusIconValues
      )
    )
    addOption(
      StringOption(
        IjOptionConstants.ideavimsupport,
        IjOptionConstants.ideavimsupport,
        "dialog",
        isList = true,
        IjOptionConstants.ideavimsupportValues
      )
    )
    addOption(
      StringOption(
        IjOptionConstants.ideawrite,
        IjOptionConstants.ideawrite,
        "all",
        isList = false,
        IjOptionConstants.ideaWriteValues
      )
    )
    addOption(
      StringOption(
        IjOptionConstants.lookupkeys,
        IjOptionConstants.lookupkeys,
        "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>",
        isList = true
      )
    )
    addOption(ToggleOption(IjOptionConstants.octopushandler, IjOptionConstants.octopushandler, false))
    addOption(ToggleOption(IjOptionConstants.oldundo, IjOptionConstants.oldundo, true))
    addOption(ToggleOption(IjOptionConstants.trackactionids, "tai", false))
    addOption(UnsignedNumberOption(IjOptionConstants.visualdelay, IjOptionConstants.visualdelay, 100))

    // TODO: Figure out how to nicely override clipboard's default value for the IntelliJ specific ideaput value
    // This options overrides Vim's default value, so we keep it here
//    addOption(
//      StringOption(
//        OptionConstants.clipboard,
//        "cb",
//        "ideaput,autoselect,exclude:cons\\|linux",
//        isList = true
//      )
//    )

    addOption(ToggleOption("unifyjumps", "unifyjumps", true))
  }

  // This needs to be Option<out VimDataType> so that it can work with derived option types, such as NumberOption, which
  // derives from Option<VimInt>
  private fun <T : Option<out VimDataType>> addOption(option: T) = option.also { Options.addOption(option) }
}