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
    ToggleOption(closenotebooks, closenotebooks, true),
    StringOption(ide, ide, ApplicationNamesInfo.getInstance().fullProductNameWithEdition),
    ToggleOption(ideacopypreprocess, ideacopypreprocess, false),
    ToggleOption(ideajoin, ideajoin, false),
    ToggleOption(ideamarks, ideamarks, true),
    StringOption(idearefactormode, idearefactormode, "select", isList = false, ideaRefactorModeValues),
    StringOption(ideastatusicon, ideastatusicon, "enabled", isList = false, ideaStatusIconValues),
    StringOption(ideavimsupport, ideavimsupport, "dialog", isList = true, ideavimsupportValues),
    StringOption(ideawrite, ideawrite, "all", isList = false, ideaWriteValues),
    StringOption(lookupkeys, lookupkeys, "<Tab>,<Down>,<Up>,<Enter>,<Left>,<Right>,<C-Down>,<C-Up>,<PageUp>,<PageDown>,<C-J>,<C-Q>", isList = true),
    ToggleOption(oldundo, oldundo, true),
    ToggleOption(trackactionids, "tai", false),
    UnsignedNumberOption(visualdelay, visualdelay, 100),

    // This options overrides Vim's default value, so we keep it here
    StringOption(OptionConstants.clipboard, OptionConstants.clipboardAlias, "ideaput,autoselect,exclude:cons\\|linux", isList = true),
  )

  init {
    customOptions.forEach {
      addOption(it)
    }
  }

  @Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate")
  companion object {

    // IJ specific options
    const val closenotebooks = "closenotebooks"
    const val ide = "ide"
    const val ideacopypreprocess = "ideacopypreprocess"
    const val ideajoin = "ideajoin"
    const val ideamarks = "ideamarks"

    const val idearefactormode = "idearefactormode"
    const val idearefactormode_keep = "keep"
    const val idearefactormode_select = "select"
    const val idearefactormode_visual = "visual"

    const val ideastatusicon = "ideastatusicon"
    const val ideastatusicon_enabled = "enabled"
    const val ideastatusicon_gray = "gray"
    const val ideastatusicon_disabled = "disabled"

    const val ideavimsupport = "ideavimsupport"
    const val ideavimsupport_dialog = "dialog"
    const val ideavimsupport_singleline = "singleline"
    const val ideavimsupport_dialoglegacy = "dialoglegacy"

    const val ideawrite = "ideawrite"
    const val ideawrite_all = "all"
    const val ideawrite_file = "file"

    const val lookupkeys = "lookupkeys"
    const val oldundo = "oldundo"
    const val trackactionids = "trackactionids"
    const val visualdelay = "visualdelay"

    val ideaStatusIconValues = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    val ideaRefactorModeValues = setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    val ideaWriteValues = setOf(ideawrite_all, ideawrite_file)
    val ideavimsupportValues = setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}