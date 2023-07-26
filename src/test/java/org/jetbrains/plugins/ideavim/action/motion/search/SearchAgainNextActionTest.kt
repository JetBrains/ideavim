/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.search

import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.newapi.IjVimEditor
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SearchAgainNextActionTest : VimTestCase() {
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

  @Test
  fun `test search unfolds text`() {
    configureByJavaText("""
      // My long comment that will be the longest line in the text
      /* 
      pu${c}pa
      */
    """.trimIndent())

    val foldingModel = fixture.editor.foldingModel
    foldingModel.runBatchFoldingOperation {
      val foldRegion = foldingModel.addFoldRegion(61, 71, "pupa")
      foldRegion!!.isExpanded = false
    }
    assertEquals(2, IjVimEditor(fixture.editor).getVisualLineCount())

    typeText("/pupa<CR>")
    assertEquals(4, IjVimEditor(fixture.editor).getVisualLineCount())
  }
}
