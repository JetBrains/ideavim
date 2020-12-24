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

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import junit.framework.Assert
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
    typeTextInFile(parseKeys("yW"), file)
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
    typeText(parseKeys("yy"))

    TestCase.assertEquals(initialOffset, myFixture.editor.caretModel.offset)
  }

  fun `test unnamed saved to " register`() {
    val clipboardValue = OptionsManager.clipboard.value
    OptionsManager.clipboard.set(ClipboardOptionsData.unnamed)

    try {
      configureByText("I found it in a ${c}legendary land")
      typeText(parseKeys("yiw"))

      val starRegister = VimPlugin.getRegister().getRegister('*') ?: kotlin.test.fail("Register * is empty")
      Assert.assertEquals("legendary", starRegister.text)

      val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
      Assert.assertEquals("legendary", quoteRegister.text)
    } finally {
      OptionsManager.clipboard.set(clipboardValue)
    }
  }

  fun `test z saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText(parseKeys("\"zyiw"))

    val starRegister = VimPlugin.getRegister().getRegister('z') ?: kotlin.test.fail("Register z is empty")
    Assert.assertEquals("legendary", starRegister.text)

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    Assert.assertEquals("legendary", quoteRegister.text)
  }

  fun `test " saved to " register`() {
    configureByText("I found it in a ${c}legendary land")
    typeText(parseKeys("\"zyiw"))

    val quoteRegister = VimPlugin.getRegister().getRegister('"') ?: kotlin.test.fail("Register \" is empty")
    Assert.assertEquals("legendary", quoteRegister.text)
  }

  fun `test yank up`() {
    val file = """
            A ${c}Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(parseKeys("yk"), file)

    Assert.assertTrue(VimPlugin.isError())
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
    typeTextInFile(parseKeys("y$"), file)
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
    typeTextInFile(parseKeys("\"*yiw"), file)
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
    typeTextInFile(parseKeys("\"*"), file)
    Assert.assertNull(VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test yank last line`() {
    val file = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent${c} of a mountain pass.
        """.trimIndent()

    doTest("yy", file, file, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    val text = VimPlugin.getRegister().lastRegister?.text ?: kotlin.test.fail()

    TestCase.assertEquals("hard by the torrent of a mountain pass.\n", text)
  }
}
