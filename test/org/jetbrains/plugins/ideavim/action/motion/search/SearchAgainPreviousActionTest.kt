/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase
import java.util.*

class SearchAgainPreviousActionTest : VimTestCase() {
  fun `test search with tabs`() {
    val before = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...${c}all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    val keys = parseKeys("N")
    val after = """
  I found it in a legendary land
  ...${c}all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }

  fun `test search with tabs 2`() {
    val before = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was .${c}all settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    val keys = parseKeys("N")
    val after = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...${c}all it was .all settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }

  fun `test search with tabs 3`() {
    val before = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was .all.${c}all settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    val keys = parseKeys("N")
    val after = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was .${c}all.all settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }

  fun `test search with tabs with wrap`() {
    val before = """
  I found it in a legendary land
  ...${c}all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    val keys = parseKeys("N")
    val after = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...${c}all by the torrent of a mountain pass
""".trimIndent().dotToTab()
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }
}
