/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.application.ApplicationNamesInfo
import com.maddyhome.idea.vim.api.VimOptionServiceBase
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.UnsignedNumberOption

internal class IjVimOptionService : VimOptionServiceBase() {

  private val customOptions = setOf(
    ToggleOption("closenotebooks", "closenotebooks", true),
    StringOption(ideName, ideAlias, ApplicationNamesInfo.getInstance().fullProductNameWithEdition),
    ToggleOption(ideacopypreprocessName, ideacopypreprocessAlias, false),
    ToggleOption(ideajoinName, ideajoinAlias, false),
    ToggleOption(ideamarksName, ideamarksAlias, true),
    StringOption(idearefactormodeName, idearefactormodeAlias, "select", isList = false, ideaRefactorModeValues),
    StringOption(ideastatusiconName, ideastatusiconAlias, "enabled", isList = false, ideaStatusIconValues),
    StringOption(ideavimsupportName, ideavimsupportAlias, "dialog", isList = true, ideavimsupportValues),
    StringOption(ideawriteName, ideawriteAlias, "all", isList = false, ideaWriteValues),
    StringOption(lookupkeysName, lookupkeysAlias, "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>", isList = true),
    ToggleOption(oldUndo, oldUndo, true),
    ToggleOption(trackactionidsName, trackactionidsAlias, false),
    UnsignedNumberOption(visualdelayName, visualdelayAlias, 100),

    // This options overrides Vim's default value, so we keep it here
    StringOption(OptionConstants.clipboardName, OptionConstants.clipboardAlias, "ideaput,autoselect,exclude:cons\\|linux", isList = true),
  )

  init {
    customOptions.forEach {
      addOption(it)
    }
  }

  companion object {
    const val oldUndo = "oldundo"

    const val ideName = "ide"
    const val ideAlias = "ide"

    const val ideacopypreprocessName = "ideacopypreprocess"
    const val ideacopypreprocessAlias = "ideacopypreprocess"

    const val ideajoinName = "ideajoin"
    const val ideajoinAlias = "ideajoin"

    const val ideamarksName = "ideamarks"
    const val ideamarksAlias = "ideamarks"

    const val idearefactormodeName = "idearefactormode"
    const val idearefactormodeAlias = "idearefactormode"
    const val idearefactormode_keep = "keep"
    const val idearefactormode_select = "select"
    const val idearefactormode_visual = "visual"

    const val ideastatusiconName = "ideastatusicon"
    const val ideastatusiconAlias = "ideastatusicon"
    const val ideastatusicon_enabled = "enabled"
    const val ideastatusicon_gray = "gray"
    const val ideastatusicon_disabled = "disabled"

    const val ideawriteName = "ideawrite"
    const val ideawriteAlias = "ideawrite"
    const val ideawrite_all = "all"
    const val ideawrite_file = "file"

    const val ideavimsupportName = "ideavimsupport"
    const val ideavimsupportAlias = "ideavimsupport"
    const val ideavimsupport_dialog = "dialog"
    const val ideavimsupport_singleline = "singleline"
    const val ideavimsupport_dialoglegacy = "dialoglegacy"

    const val lookupkeysName = "lookupkeys"
    const val lookupkeysAlias = "lookupkeys"

    const val trackactionidsName = "trackactionids"
    const val trackactionidsAlias = "tai"

    const val visualdelayName = "visualdelay"
    const val visualdelayAlias = "visualdelay"

    val ideaStatusIconValues = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    val ideaRefactorModeValues = setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    val ideaWriteValues = setOf(ideawrite_all, ideawrite_file)
    val ideavimsupportValues = setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}
