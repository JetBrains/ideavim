/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class YankLinesCommandTest : VimTestCase() {
  @Test
  fun `test copy with range`() {
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                h
      """.trimIndent(),
    )
    typeText(commandToKeys("3,4y"))
    val yanked = VimPlugin.getRegister().lastRegister!!.text
    kotlin.test.assertEquals(
      """|Lorem ipsum dolor sit amet,
         |consectetur adipiscing elit
         |
      """.trimMargin(),
      yanked,
    )
  }

  @Test
  fun `test copy with one char on the last line`() {
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                h
      """.trimIndent(),
    )
    typeText(commandToKeys("%y"))
    val yanked = VimPlugin.getRegister().lastRegister!!.text
    kotlin.test.assertEquals(
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                h
                
      """.trimIndent(),
      yanked,
    )
  }
}
