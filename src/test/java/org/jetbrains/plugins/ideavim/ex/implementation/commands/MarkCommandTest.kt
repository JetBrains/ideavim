/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.fail

/**
 * @author Alex Plate
 */
class MarkCommandTest : VimTestCase() {
  @Test
  fun `test simple mark`() {
    configureByText(
      """Lorem ipsum dolor sit amet,
                         |consectetur adipiscing elit
                         |where it$c was settled on some sodden sand
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(commandToKeys("mark a"))
    val vimEditor = fixture.editor.vim
    injector.markService.getMark(vimEditor.primaryCaret(), 'a')?.let {
      kotlin.test.assertEquals(2, it.line)
      kotlin.test.assertEquals(0, it.col)
    } ?: fail("Mark is null")
  }

  @Test
  fun `test global mark`() {
    configureByText(
      """Lorem ipsum dolor sit amet,
                         |consectetur adipiscing elit
                         |where it$c was settled on some sodden sand
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(commandToKeys("mark G"))
    val vimEditor = fixture.editor.vim
    injector.markService.getMark(vimEditor.primaryCaret(), 'G')?.let {
      kotlin.test.assertEquals(2, it.line)
      kotlin.test.assertEquals(0, it.col)
    } ?: fail("Mark is null")
  }

  @Test
  fun `test k mark`() {
    configureByText(
      """Lorem ipsum dolor sit amet,
                         |consectetur adipiscing elit
                         |where it$c was settled on some sodden sand
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(commandToKeys("k a"))
    val vimEditor = fixture.editor.vim
    injector.markService.getMark(vimEditor.primaryCaret(), 'a')?.let {
      kotlin.test.assertEquals(2, it.line)
      kotlin.test.assertEquals(0, it.col)
    } ?: fail("Mark is null")
  }

  @Test
  fun `test k mark with no whitespace`() {
    configureByText(
      """Lorem ipsum dolor sit amet,
                         |consectetur adipiscing elit
                         |where it$c was settled on some sodden sand
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(commandToKeys("ka"))
    val vimEditor = fixture.editor.vim
    injector.markService.getMark(vimEditor.primaryCaret(), 'a')?.let {
      kotlin.test.assertEquals(2, it.line)
      kotlin.test.assertEquals(0, it.col)
    } ?: fail("Mark is null")
  }

  @Test
  fun `test mark in range`() {
    configureByText(
      """Lorem ipsum dolor sit amet,
                         |consectetur adipiscing elit
                         |where it$c was settled on some sodden sand
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(commandToKeys("1,2 mark a"))
    val vimEditor = fixture.editor.vim
    injector.markService.getMark(vimEditor.primaryCaret(), 'a')?.let {
      kotlin.test.assertEquals(1, it.line)
      kotlin.test.assertEquals(0, it.col)
    } ?: fail("Mark is null")
  }
}
