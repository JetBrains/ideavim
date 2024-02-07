/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FileGetLocationInfoActionTest : VimTestCase() {
  @VimBehaviorDiffers(originalVimAfter = "Col 1 of 11; Line 1 of 6; Word 1 of 32; Byte 1 of 166")
  @Test
  fun `test get file info`() {
    val keys = injector.parser.parseKeys("g<C-G>")
    val before = """
            ${c}Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(before)
    typeText(keys)
    kotlin.test.assertEquals("Col 1 of 11; Line 1 of 6; Word 1 of 23; Character 1 of 128", VimPlugin.getMessage())
  }

  @VimBehaviorDiffers(originalVimAfter = "Col 1 of 11; Line 1 of 7; Word 1 of 32; Byte 1 of 167")
  @Test
  fun `test get file info with empty line`() {
    val keys = injector.parser.parseKeys("g<C-G>")
    val before = """
            ${c}Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            
    """.trimIndent()
    configureByText(before)
    typeText(keys)
    kotlin.test.assertEquals("Col 1 of 11; Line 1 of 7; Word 1 of 24; Character 1 of 129", VimPlugin.getMessage())
  }

  @VimBehaviorDiffers(originalVimAfter = "Col 1 of 40; Line 4 of 7; Word 12 of 32; Byte 55 of 167")
  @Test
  fun `test get file info in the middle`() {
    val keys = injector.parser.parseKeys("g<C-G>")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            all rocks ${c}and lavender and tufted grass,
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            
    """.trimIndent()
    configureByText(before)
    typeText(keys)
    kotlin.test.assertEquals("Col 11 of 40; Line 4 of 7; Word 11 of 28; Character 52 of 142", VimPlugin.getMessage())
  }

  @VimBehaviorDiffers(originalVimAfter = "Col 1 of 0; Line 7 of 7; Word 32 of 32; Byte 167 of 167")
  @Test
  fun `test get file info on the last line`() {
    val keys = injector.parser.parseKeys("g<C-G>")
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            $c
    """.trimIndent()
    configureByText(before)
    typeText(keys)
    kotlin.test.assertEquals("Col 1 of 1; Line 7 of 7; Word 24 of 24; Character 130 of 129", VimPlugin.getMessage())
  }
}
