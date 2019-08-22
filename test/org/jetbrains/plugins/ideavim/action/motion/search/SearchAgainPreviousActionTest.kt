package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase
import java.util.*

class SearchAgainPreviousActionTest : VimTestCase() {
  fun `test search with tabs`() {
    val before = dotToTab("""
      I found it in a legendary land
      ...all rocks and lavender and tufted grass,
      ...${c}all it was settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    val keys = parseKeys("N")
    val after = dotToTab("""
      I found it in a legendary land
      ...${c}all rocks and lavender and tufted grass,
      ...all it was settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }

  fun `test search with tabs 2`() {
    val before = dotToTab("""
      I found it in a legendary land
      ...all rocks and lavender and tufted grass,
      ...all it was .${c}all settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    val keys = parseKeys("N")
    val after = dotToTab("""
      I found it in a legendary land
      ...all rocks and lavender and tufted grass,
      ...${c}all it was .all settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }

  fun `test search with tabs 3`() {
    val before = dotToTab("""
      I found it in a legendary land
      ...all rocks and lavender and tufted grass,
      ...all it was .all.${c}all settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    val keys = parseKeys("N")
    val after = dotToTab("""
      I found it in a legendary land
      ...all rocks and lavender and tufted grass,
      ...all it was .${c}all.all settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }

  fun `test search with tabs with wrap`() {
    val before = dotToTab("""
      I found it in a legendary land
      ...${c}all rocks and lavender and tufted grass,
      ...all it was settled on some sodden sand
      ...all by the torrent of a mountain pass
    """.trimIndent())
    val keys = parseKeys("N")
    val after = dotToTab("""
      I found it in a legendary land
      ...all rocks and lavender and tufted grass,
      ...all it was settled on some sodden sand
      ...${c}all by the torrent of a mountain pass
    """.trimIndent())
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.getSearch()
        .search(it, it.caretModel.primaryCaret, "all", 1, EnumSet.of(CommandFlags.FLAG_SEARCH_FWD), false)
    }
  }
}