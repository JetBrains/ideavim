/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class YankMotionActionTest : VimTestCase() {
  @Test
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

    kotlin.test.assertEquals("and", text)
  }

  @Test
  fun `test yank caret doesn't move`() {
    val file = """
            A Discovery

            I found it in a legendary l${c}and
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(file)

    val initialOffset = fixture.editor.caretModel.offset
    typeText("yy")

    kotlin.test.assertEquals(initialOffset, fixture.editor.caretModel.offset)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  @Test
  fun `test unnamed saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    enterCommand("set clipboard=unnamed")
    typeText("yiw")

    val starRegister = VimPlugin.getRegister().getRegister('*') ?: kotlin.test.fail("Register * is empty")
    kotlin.test.assertEquals("legendary", starRegister.text)

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    kotlin.test.assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  @Test
  fun `test z saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText("\"zyiw")

    val starRegister = VimPlugin.getRegister().getRegister('z') ?: kotlin.test.fail("Register z is empty")
    kotlin.test.assertEquals("legendary", starRegister.text)

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    kotlin.test.assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  @Test
  fun `test " saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText("\"zyiw")

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    kotlin.test.assertEquals("legendary", quoteRegister.text)
  }

  @Test
  fun `test yank up`() {
    val file = """
            A ${c}Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile("yk", file)

    kotlin.test.assertTrue(injector.messages.isError())
  }

  @Test
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

    kotlin.test.assertEquals("", text)
  }

  @Test
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

    kotlin.test.assertEquals("legendary", text)
  }

  @Test
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
    kotlin.test.assertNull(VimPlugin.getRegister().lastRegister?.text)
  }

  @Test
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

    kotlin.test.assertEquals("hard by the torrent of a mountain pass.\n", text)
  }
}
