/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase

class YankLinesCommandTest : VimTestCase() {
  fun `test copy with range`() {
    configureByText(
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                h
      """.trimIndent()
    )
    typeText(commandToKeys("3,4y"))
    val yanked = VimPlugin.getRegister().lastRegister!!.text
    assertEquals(
      """|I found it in a legendary land
         |all rocks and lavender and tufted grass,
         |
         """.trimMargin(),
      yanked
    )
  }

  fun `test copy with one char on the last line`() {
    configureByText(
      """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                h
      """.trimIndent()
    )
    typeText(commandToKeys("%y"))
    val yanked = VimPlugin.getRegister().lastRegister!!.text
    assertEquals(
      """
                A Discovery

                I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                h
                
      """.trimIndent(),
      yanked
    )
  }
}
