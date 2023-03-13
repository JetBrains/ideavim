/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * @author Jørgen Granseth
 */
class DeleteMarksCommandTest : VimTestCase() {
  private fun setUpMarks(marks: String) {
    configureByText(
      """I found it in a legendary land
         all rocks and lavender and tufted grass,
         where it was settled on some sodden sand
         hard by the torrent of a mountain pass.

         The features it combines mark it as new
         to science: shape and shade -- the special tinge,
         akin to moonlight, tempering its blue,
         the dingy underside, the checquered fringe.

         My needles have teased out its sculpted sex;
         corroded tissues could no longer hide
         that priceless mote now dimpling the convex
         and limpid teardrop on a lighted slide.
      """.trimMargin(),
    )

    val vimEditor = myFixture.editor.vim
    marks.forEachIndexed { index, c ->
      injector.markService
        .setMark(vimEditor.primaryCaret(), c, myFixture.editor.logicalPositionToOffset(LogicalPosition(index, 0)))
    }
  }

  private fun getMark(ch: Char): Mark? {
    val vimEditor = myFixture.editor.vim
    return injector.markService.getMark(vimEditor.primaryCaret(), ch)
  }

  fun `test delete single mark`() {
    setUpMarks("a")
    typeText(commandToKeys("delmarks a"))

    assertNull(getMark('a'), "Mark was not deleted")
  }

  fun `test delete multiple marks`() {
    setUpMarks("abAB")
    typeText(commandToKeys("delmarks Ab"))

    arrayOf('A', 'b')
      .forEach { ch -> assertNull(getMark(ch), "Mark $ch was not deleted") }

    arrayOf('a', 'B')
      .forEach { ch -> assertNotNull(getMark(ch), "Mark $ch was unexpectedly deleted") }
  }

  fun `test delete ranges (inclusive)`() {
    setUpMarks("abcde")
    typeText(commandToKeys("delmarks b-d"))

    arrayOf('b', 'c', 'd')
      .forEach { ch -> assertNull(getMark(ch), "Mark $ch was not deleted") }

    arrayOf('a', 'e')
      .forEach { ch -> assertNotNull(getMark(ch), "Mark $ch was unexpectedly deleted") }
  }

  fun `test delete multiple ranges and marks with whitespace`() {
    setUpMarks("abcdeABCDE")
    typeText(commandToKeys("delmarks b-dC-E a"))

    arrayOf('a', 'b', 'c', 'd', 'C', 'D', 'E', 'a')
      .forEach { ch -> assertNull(getMark(ch), "Mark $ch was not deleted") }

    arrayOf('e', 'A', 'B')
      .forEach { ch -> assertNotNull(getMark(ch), "Mark $ch was unexpectedly deleted") }
  }

  fun `test invalid range throws exception without deleting any marks`() {
    setUpMarks("a")
    typeText(commandToKeys("delmarks a-C"))
    assertPluginError(true)

    assertNotNull(getMark('a'), "Mark was deleted despite invalid command given")
  }

  fun `test invalid characters throws exception`() {
    setUpMarks("a")
    typeText(commandToKeys("delmarks bca# foo"))
    assertPluginError(true)

    assertNotNull(getMark('a'), "Mark was deleted despite invalid command given")
  }

  fun `test delmarks! with trailing spaces`() {
    setUpMarks("aBcAbC")
    typeText(commandToKeys("delmarks!"))

    arrayOf('a', 'b', 'c')
      .forEach { ch -> assertNull(getMark(ch), "Mark $ch was not deleted") }

    arrayOf('A', 'B', 'C')
      .forEach { ch -> assertNotNull(getMark(ch), "Global mark $ch was deleted by delmarks!") }
  }

  fun `test delmarks! with other arguments fails`() {
    setUpMarks("aBcAbC")
    typeText(commandToKeys("delmarks!a"))

    assertPluginError(true)
    arrayOf('a', 'b', 'c', 'A', 'B', 'C')
      .forEach { ch -> assertNotNull(getMark(ch), "Mark $ch was deleted despite invalid command given") }
  }

  fun `test trailing spaces ignored`() {
    setUpMarks("aBcAbC")
    typeText(commandToKeys("delmarks!   "))

    arrayOf('a', 'b', 'c')
      .forEach { ch -> assertNull(getMark(ch), "Mark $ch was not deleted") }

    arrayOf('A', 'B', 'C')
      .forEach { ch -> assertNotNull(getMark(ch), "Global mark $ch was deleted by delmarks!") }
  }

  fun `test alias (delm)`() {
    setUpMarks("a")
    typeText(commandToKeys("delm a"))

    assertNull(getMark('a'), "Mark was not deleted")

    setUpMarks("aBcAbC")
    typeText(commandToKeys("delm!"))

    arrayOf('a', 'b', 'c')
      .forEach { ch -> assertNull(getMark(ch), "Mark $ch was not deleted") }

    arrayOf('A', 'B', 'C')
      .forEach { ch -> assertNotNull(getMark(ch), "Global mark $ch was deleted by delm!") }
  }
}
