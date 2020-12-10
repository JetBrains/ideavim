/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

import org.jetbrains.plugins.ideavim.VimTestCase

class SearchAgainNextActionTest : VimTestCase() {
  fun `test search next after search command with offset`() {
    val before = """
      ${c}I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
      
      The features it combines mark it as new
      to science: shape and shade -- the special tinge,
      akin to moonlight, tempering its blue,
      the dingy underside, the checquered fringe.""".trimIndent()
    val after = """
      I found it in a legendary land
      all rocks and lavender and tufted grass,
      where it was settled on some sodden sand
      hard by the torrent of a mountain pass.
      
      The features it combines mark it as new
      to science: shape and shade -- the special tinge,
      akin to moonlight, tempering its blue,
      ${c}the dingy underside, the checquered fringe.""".trimIndent()

    // Should search again for the search pattern, and apply offset
    doTest(listOf(searchCommand("/and/3"), "n"), before, after)
  }

  fun `test search next after substitute command`() {
    val before = """
       I found it in a legendary land
       all rocks and lavender and tufted grass,
       ${c}where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features it combines mark it as new
       to science: shape and shade -- the special tinge,
       akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.""".trimIndent()
    val after = """
       I found it in a legendary land
       all rocks and lavender and tufted grass,
       where I was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features ${c}it combines mark it as new
       to science: shape and shade -- the special tinge,
       akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.""".trimIndent()

    // Should search again for the substitution pattern
    doTest(listOf(exCommand("s/it/I"), "n"), before, after)
  }

  fun `test search next after substitute does not reset search offset`() {
    val before = """
       ${c}I found it in a legendary land
       all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features it combines mark it as new
       to science: shape and shade -- the special tinge,
       akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.""".trimIndent()
    val after = """
       I found it in a legendary land
       all rocks and lavender and tufted grass,
       where I was settled on some sodden sand
       hard by the torrent of a mountain pass.
    
       The features it combines mark it as new
       to science: shape and shade -- the special tinge,
       ${c}akin to moonlight, tempering its blue,
       the dingy underside, the checquered fringe.""".trimIndent()

    // Should search again for the substitution pattern, but apply offset from search
    doTest(listOf(searchCommand("/land/2"), exCommand("s/it/I"), "n"), before, after)
  }
}