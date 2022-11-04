/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.vim
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class MarkCommandTest : VimTestCase() {
  fun `test simple mark`() {
    configureByText(
      """I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin()
    )
    typeText(commandToKeys("mark a"))
    VimPlugin.getMark().getMark(myFixture.editor.vim, 'a')?.let {
      assertEquals(2, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }

  fun `test global mark`() {
    configureByText(
      """I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin()
    )
    typeText(commandToKeys("mark G"))
    VimPlugin.getMark().getMark(myFixture.editor.vim, 'G')?.let {
      assertEquals(2, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }

  fun `test k mark`() {
    configureByText(
      """I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin()
    )
    typeText(commandToKeys("k a"))
    VimPlugin.getMark().getMark(myFixture.editor.vim, 'a')?.let {
      assertEquals(2, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }

  fun `test mark in range`() {
    configureByText(
      """I found it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it$c was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin()
    )
    typeText(commandToKeys("1,2 mark a"))
    VimPlugin.getMark().getMark(myFixture.editor.vim, 'a')?.let {
      assertEquals(1, it.logicalLine)
      assertEquals(0, it.col)
    } ?: TestCase.fail("Mark is null")
  }
}
