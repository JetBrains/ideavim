/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class YankMotionActionTest : VimTestCase() {
  fun `test yank till new line`() {
    val file = """
            A Discovery

            I found it in a legendary l${c}and
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile("yW", file)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    TestCase.assertEquals("and", text)
  }

  fun `test yank caret doesn't move`() {
    val file = """
            A Discovery

            I found it in a legendary l${c}and
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(file)

    val initialOffset = myFixture.editor.caretModel.offset
    typeText("yy")

    TestCase.assertEquals(initialOffset, myFixture.editor.caretModel.offset)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  fun `test unnamed saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    enterCommand("set clipboard=unnamed")
    typeText("yiw")

    val starRegister = VimPlugin.getRegister().getRegister('*') ?: kotlin.test.fail("Register * is empty")
    assertEquals("legendary", starRegister.text)

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  fun `test z saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText("\"zyiw")

    val starRegister = VimPlugin.getRegister().getRegister('z') ?: kotlin.test.fail("Register z is empty")
    assertEquals("legendary", starRegister.text)

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  fun `test " saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText("\"zyiw")

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    assertEquals("legendary", quoteRegister.text)
  }

  fun `test yank up`() {
    val file = """
            A ${c}Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile("yk", file)

    assertTrue(VimPlugin.isError())
  }

  fun `test yank dollar at last empty line`() {
    val file = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            $c
    """.trimIndent()
    typeTextInFile("y$", file)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    TestCase.assertEquals("", text)
  }

  fun `test yank to star with mapping`() {
    val file = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(commandToKeys("map * *zz"), file)
    typeTextInFile("\"*yiw", file)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    TestCase.assertEquals("legendary", text)
  }

  fun `test yank to star with yank mapping`() {
    val file = """
            A Discovery

            I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(commandToKeys("map * *yiw"), file)
    typeTextInFile("\"*", file)
    assertNull(VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test yank last line`() {
    val file = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent$c of a mountain pass.
    """.trimIndent()

    doTest("yy", file, file)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    assertEquals("hard by the torrent of a mountain pass.\n", text)
  }
}
