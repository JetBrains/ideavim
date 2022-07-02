/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.application.ApplicationNamesInfo
import com.maddyhome.idea.vim.api.VimOptionServiceBase
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.option.ToggleOption

internal class IjVimOptionService : VimOptionServiceBase() {

  private val customOptions = setOf(
    ToggleOption(oldUndo, oldUndo, false),
    ToggleOption(ideajoinName, ideajoinAlias, false),
    ToggleOption(ideamarksName, ideamarksAlias, true),
    StringOption(ideName, ideAlias, ApplicationNamesInfo.getInstance().fullProductNameWithEdition),
    StringOption(idearefactormodeName, idearefactormodeAlias, "select", isList = false, ideaRefactorModeValues),
    StringOption(ideastatusiconName, ideastatusiconAlias, "enabled", isList = false, ideaStatusIconValues),
    StringOption(ideawriteName, ideawriteAlias, "all", isList = false, ideaWriteValues),
    StringOption(ideavimsupportName, ideavimsupportAlias, "dialog", isList = true, ideavimsupportValues),

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

    val ideaStatusIconValues = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    val ideaRefactorModeValues = setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    val ideaWriteValues = setOf(ideawrite_all, ideawrite_file)
    val ideavimsupportValues = setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}
