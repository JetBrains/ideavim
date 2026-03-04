/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

class IjOptionConstants {
  @Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate", "ConstPropertyName")
  companion object {

    const val idearefactormode_keep: String = "keep"
    const val idearefactormode_select: String = "select"
    const val idearefactormode_visual: String = "visual"

    const val ideastatusicon_enabled: String = "enabled"
    const val ideastatusicon_gray: String = "gray"
    const val ideastatusicon_disabled: String = "disabled"

    const val ideavimsupport_dialog: String = "dialog"
    const val ideavimsupport_singleline: String = "singleline"
    const val ideavimsupport_dialoglegacy: String = "dialoglegacy"

    const val ideawrite_all: String = "all"
    const val ideawrite_file: String = "file"

    val ideaStatusIconValues: Set<String> = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    val ideaRefactorModeValues: Set<String> =
      setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    val ideaWriteValues: Set<String> = setOf(ideawrite_all, ideawrite_file)
    val ideavimsupportValues: Set<String> =
      setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}
