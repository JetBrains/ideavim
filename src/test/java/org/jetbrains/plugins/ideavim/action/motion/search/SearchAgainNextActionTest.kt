/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SearchAgainNextActionTest : VimTestCase() {
  @Test
  fun `test search next updates status line`() {
    doTest(
      listOf(searchCommand("/ipsum"), "n"),
      """
         ${c}Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
      """.trimIndent(),
      """
         Lorem ipsum dolor sit amet,
         Lorem ${c}ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
      """.trimIndent(),
    )
    assertStatusLineMessageContains("/ipsum")
  }

  @Test
  fun `test search next for backwards search updates status line correctly`() {
    doTest(
      listOf(searchCommand("?ipsum"), "n"),
      """
         Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
         ${c}Lorem ipsum dolor sit amet,
      """.trimIndent(),
      """
         Lorem ${c}ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
         Lorem ipsum dolor sit amet,
      """.trimIndent(),
    )
    assertStatusLineMessageContains("?ipsum")
  }

  @Test
  fun `test search next after search command with offset`() {
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
      akin to moonlight, tempering its blue,
      ${c}the dingy underside, the checquered fringe.
    """.trimIndent()

    // Should search again for the search pattern, and apply offset
    doTest(listOf(searchCommand("/and/3"), "n"), before, after)
  }

  @Test
  fun `test search next after substitute command`() {
    val before = """
       I found it in a legendary land
       all rocks and lavender and tufted grass,
       ${c}where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features it combines mark it as new
       to science: shape and shade -- the special tinge,
       akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.
    """.trimIndent()
    val after = """
       I found it in a legendary land
       all rocks and lavender and tufted grass,
       where I was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features ${c}it combines mark it as new
       to science: shape and shade -- the special tinge,
       akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.
    """.trimIndent()

    // Should search again for the substitution pattern
    doTest(listOf(exCommand("s/it/I"), "n"), before, after)
  }

  @Test
  fun `test search next after substitute does not reset search offset`() {
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
       where I was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features it combines mark it as new
       to science: shape and shade -- the special tinge,
       ${c}akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.
    """.trimIndent()

    // Should search again for the substitution pattern, but apply offset from search
    doTest(listOf(searchCommand("/land/2"), exCommand("s/it/I"), "n"), before, after)
  }
}
