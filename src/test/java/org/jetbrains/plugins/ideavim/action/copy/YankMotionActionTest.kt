/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
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
    typeTextInFile(injector.parser.parseKeys("yW"), file)
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
    typeText(injector.parser.parseKeys("yy"))

    TestCase.assertEquals(initialOffset, myFixture.editor.caretModel.offset)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  fun `test unnamed saved to " register`() {
    val clipboardValue = (VimPlugin.getOptionService().getOptionValue(OptionScope.GLOBAL, OptionConstants.clipboardName) as VimString).value
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.clipboardName, VimString("unnamed"))

    try {
      configureByText("I found it in a ${c}legendary land")
      typeText(injector.parser.parseKeys("yiw"))

      val starRegister = VimPlugin.getRegister().getRegister('*') ?: kotlin.test.fail("Register * is empty")
      assertEquals("legendary", starRegister.text)

      val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
      assertEquals("legendary", quoteRegister.text)
    } finally {
      VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.clipboardName, VimString(clipboardValue))
    }
  }

  @Suppress("DANGEROUS_CHARACTERS")
  fun `test z saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText(injector.parser.parseKeys("\"zyiw"))

    val starRegister = VimPlugin.getRegister().getRegister('z') ?: kotlin.test.fail("Register z is empty")
    assertEquals("legendary", starRegister.text)

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    assertEquals("legendary", quoteRegister.text)
  }

  @Suppress("DANGEROUS_CHARACTERS")
  fun `test " saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText(injector.parser.parseKeys("\"zyiw"))

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
    typeTextInFile(injector.parser.parseKeys("yk"), file)

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
    typeTextInFile(injector.parser.parseKeys("y$"), file)
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
    typeTextInFile(injector.parser.parseKeys("\"*yiw"), file)
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
    typeTextInFile(injector.parser.parseKeys("\"*"), file)
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

    doTest("yy", file, file, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    TestCase.assertEquals("hard by the torrent of a mountain pass.\n", text)
  }
}
