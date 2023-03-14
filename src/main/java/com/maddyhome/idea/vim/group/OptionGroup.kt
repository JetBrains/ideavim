/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.VimOptionGroupBase

internal class OptionGroup : VimOptionGroupBase() {
  init {
    IjOptions.initialise()
  }
}

internal class IjOptionConstants {
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
    const val octopushandler = "octopushandler"
    const val oldundo = "oldundo"
    const val trackactionids = "trackactionids"
    const val unifyjumps = "unifyjumps"
    const val visualdelay = "visualdelay"

    val ideaStatusIconValues = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    val ideaRefactorModeValues = setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    val ideaWriteValues = setOf(ideawrite_all, ideawrite_file)
    val ideavimsupportValues = setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}
