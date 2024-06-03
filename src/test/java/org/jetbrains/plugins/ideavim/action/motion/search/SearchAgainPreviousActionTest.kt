/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.Direction
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SearchAgainPreviousActionTest : VimTestCase() {
  @Test
  fun `test search previous updates status line`() {
    doTest(
      listOf(searchCommand("/ipsum"), "N"),
      """
         Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
         ${c}Lorem ipsum dolor sit amet,
      """.trimIndent(),
      """
         Lorem ipsum dolor sit amet,
         Lorem ${c}ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
      """.trimIndent(),
    )
    assertStatusLineMessageContains("?ipsum")
  }

  @Test
  fun `test search previous for backwards search updates status line correctly`() {
    doTest(
      listOf(searchCommand("?ipsum"), "N"),
      """
         Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
         ${c}Lorem ipsum dolor sit amet,
      """.trimIndent(),
      """
         Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
         Lorem ${c}ipsum dolor sit amet,
      """.trimIndent(),
    )
    assertStatusLineMessageContains("/ipsum")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test search with tabs`() {
    val before = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...${c}all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    val after = """
  I found it in a legendary land
  ...${c}all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    doTestWithSearch("N", before, after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test search with tabs 2`() {
    val before = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was .${c}all settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    val after = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...${c}all it was .all settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    doTestWithSearch("N", before, after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test search with tabs 3`() {
    val before = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was .all.${c}all settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    val after = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was .${c}all.all settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    doTestWithSearch("N", before, after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test search with tabs with wrap`() {
    val before = """
  I found it in a legendary land
  ...${c}all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    val after = """
  I found it in a legendary land
  ...all rocks and lavender and tufted grass,
  ...all it was settled on some sodden sand
  ...${c}all by the torrent of a mountain pass
    """.trimIndent().dotToTab()
    doTestWithSearch("N", before, after)
  }

  @Test
  fun `test search previous after search command with offset`() {
    val before = """
      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
      
      The features it combines mark it as new
      to science: shape and shade -- the special tinge,
      akin to moonlight, tempering its blue,
      the dingy underside, the checquered fringe.
    """.trimIndent()
    val after = """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
      
      The features it combines mark it as new
      to science: shape and shade -- the special tinge,
      ${c}akin to moonlight, tempering its blue,
      the dingy underside, the checquered fringe.
    """.trimIndent()

    // Should search again for the search pattern, and apply offset
    doTest(listOf(searchCommand("/and/1"), "G", "N"), before, after)
  }

  @Test
  fun `test search previous after substitute command`() {
    val before = """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      ${c}all it was settled on some sodden sand
      all by the torrent of a mountain pass
    """.trimIndent()
    val after = """
      I found it in a legendary land
      ${c}all rocks and lavender and tufted grass,
      with it was settled on some sodden sand
      all by the torrent of a mountain pass
    """.trimIndent()

    // Should search again for the substitution pattern
    doTest(listOf(exCommand("s/all/with"), "N"), before, after)
  }

  @Test
  fun `test search previous after substitute does not reset search offset`() {
    val before = """
      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
      
      The features it combines mark it as new
      to science: shape and shade -- the special tinge,
      akin to moonlight, tempering its blue,
      the dingy underside, the checquered fringe.
    """.trimIndent()
    val after = """
      I found it in a legendary land
      all rocks or lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
      
      The features it combines mark it as new
      to science: shape and shade -- the special tinge,
      ${c}akin to moonlight, tempering its blue,
      the dingy underside, the checquered fringe.
    """.trimIndent()

    // Should search again for the substitution pattern, but apply offset from search
    doTest(listOf(searchCommand("/land/1"), exCommand("s/and/or"), "G", "N"), before, after)
  }

  private fun doTestWithSearch(keys: String, before: String, after: String) {
    doTest(keys, before, after) {
      VimPlugin.getSearch().setLastSearchState("all", "", Direction.FORWARDS)
    }
  }
}
