/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.extension.replacewithregister

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.rangeOf

class ReplaceWithRegisterTest : VimTestCase() {

  override fun setUp() {
    super.setUp()
    enableExtensions("ReplaceWithRegister")
  }

  fun `test replace with empty register`() {
    val text = "one ${c}two three"
    VimPlugin.getRegister().resetRegisters()

    configureByText(text)
    typeText(parseKeys("griw"))
    myFixture.checkResult(text)
  }

  fun `test simple replace`() {
    val text = "one ${c}two three"

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("griw"))
    myFixture.checkResult("one on${c}e three")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test empty text`() {
    val text = ""

    configureByText(text)
    VimPlugin.getRegister().storeTextSpecial(RegisterGroup.UNNAMED_REGISTER, "one")
    typeText(parseKeys("griw"))
    myFixture.checkResult("on${c}e")
  }

  fun `test replace with empty text`() {
    val text = "${c}one"

    configureByText(text)
    VimPlugin.getRegister().storeTextSpecial(RegisterGroup.UNNAMED_REGISTER, "")
    typeText(parseKeys("griw"))
    myFixture.checkResult(c)
  }

  fun `test replace use different register`() {
    val text = "one ${c}two three four"

    configureByText(text)
    typeText(parseKeys("\"ayiw", "w", "\"agriw"))
    myFixture.checkResult("one two tw${c}o four")
    assertEquals("two", VimPlugin.getRegister().lastRegister?.text)
    typeText(parseKeys("w", "griw"))
    myFixture.checkResult("one two two tw${c}o")
    assertEquals("two", VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test replace use clipboard register`() {
    val text = "one ${c}two three four"

    configureByText(text)
    typeText(parseKeys("\"+yiw", "w", "\"+griw", "w", "\"+griw"))
    myFixture.checkResult("one two two tw${c}o")
    assertEquals("two", VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test replace use wrong register`() {
    val text = "one ${c}two three"

    configureByText(text)
    typeText(parseKeys("\"ayiw", "\"bgriw"))
    myFixture.checkResult(text)
  }

  fun `test replace with line`() {
    val text = """
            |I fou${c}nd it in a legendary land|
            all rocks and lavender and tufted grass,
        """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yy", "j", "griw"))
    myFixture.checkResult("""
            |I found it in a legendary land|
            all |I found it in a legendary land${c}| and lavender and tufted grass,
        """.trimIndent())
  }

  fun `test replace with line with clipboard register`() {
    val text = """
            |I fou${c}nd it in a legendary land|
            all rocks and lavender and tufted grass,
        """.trimIndent()

    configureByText(text)
    typeText(parseKeys("\"+yy", "j", "\"+griw"))
    myFixture.checkResult("""
            |I found it in a legendary land|
            all |I found it in a legendary land${c}| and lavender and tufted grass,
        """.trimIndent())
  }

  fun `test replace block selection`() {
    val text = """
            ${c}one two three
            one two three
            one two three
            one two three
        """.trimIndent()

    configureByText(text)
    typeText(parseKeys("<C-v>jjlly", "gg^w", "griw"))
    myFixture.checkResult("""
            one ${c}one three
            one onetwo three
            one onetwo three
            one two three
        """.trimIndent())
  }

  fun `test replace with number`() {
    val text = "one ${c}two three four"

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("3griw"))
    myFixture.checkResult("one on${c}e four")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  @VimBehaviorDiffers("one on${c}e on${c}e four")
  fun `test replace with multiple carets`() {
    val text = "one ${c}two ${c}three four"

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("griw"))
    myFixture.checkResult("one two one four")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test dot repeat`() {
    val text = "one ${c}two three four"

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "one", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("griw", "w", "."))
    myFixture.checkResult("one one on${c}e four")
    assertEquals("one", VimPlugin.getRegister().lastRegister?.text)
  }

  // --------------------------------------- grr --------------------------

  fun `test line replace`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("grr"))
    myFixture.checkResult("""
            ${c}legendary
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
    assertEquals("legendary", VimPlugin.getRegister().lastRegister?.text)
  }

  fun `test line replace with line`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "grr"))
    myFixture.checkResult("""
            I found it in a legendary land
            ${c}I found it in a legendary land
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
  }

  fun `test line replace with line empty line`() {
    val text = """
            I found it in ${c}a legendary land
            
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "grr"))
    myFixture.checkResult("""
            I found it in a legendary land
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
  }

  @VimBehaviorDiffers(description = "Where is the new line comes from?...")
  fun `test line replace with block`() {
    val text = """
            ${c}one two three
            one two three
            one two three
            one two three
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("<C-V>lljjyj", "grr"))
    myFixture.checkResult("""
            one two three
            ${c}one
            one
            one
            one two three
            one two three
            
    """.trimIndent())
  }

  @VimBehaviorDiffers("""
            I found it in a legendary land
            ${c}I found it in a legendary land
            hard by the torrent of a mountain pass.
  """)
  fun `test line with number`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "2grr"))
    myFixture.checkResult("""
            I found it in a legendary land
            ${c}I found it in a legendary land
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
  }

  fun `test line dot repeat`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "grr", "j", "."))
    myFixture.checkResult("""
            I found it in a legendary land
            I found it in a legendary land
            ${c}I found it in a legendary land
            hard by the torrent of a mountain pass.
    """.trimIndent())
  }

  @VimBehaviorDiffers("""
            I found it in a legendary land
            ${c}I found it in a legendary land
            where it was settled on some sodden sand
            ${c}where it was settled on some sodden sand
  """)
  fun `test line multicaret`() {
    val text = """
            I found it in ${c}a legendary land
            all rocks and lavender and tufted grass,
            where it was s${c}ettled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "grr"))
    myFixture.checkResult("""
            I found it in a legendary land
            I found it in a legendary land
            where it was settled on some sodden sand
            where it was settled on some sodden sand
            I found it in a legendary land
            where it was settled on some sodden sand

    """.trimIndent())
  }

  // ------------------------------------- gr + visual ----------------------

  fun `test visual replace`() {
    val text = """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("viw", "gr"))
    myFixture.checkResult("""
            I legendar${c}y it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
    assertEquals("legendary", VimPlugin.getRegister().lastRegister?.text)
    assertMode(CommandState.Mode.COMMAND)
  }

  fun `test visual replace with line`() {
    val text = """
            |I fo${c}und it in a legendary land|
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "viw", "gr"))
    myFixture.checkResult("""
            |I found it in a legendary land|
            all |I found it in a legendary land${c}| and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
  }

  fun `test visual replace with two lines`() {
    val text = """
            |I found it in ${c}a legendary land|
            |all rocks and lavender and tufted grass,|
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("Vjyjj3w", "viw", "gr"))
    myFixture.checkResult("""
            |I found it in a legendary land|
            |all rocks and lavender and tufted grass,|
            where it was |I found it in a legendary land|
            |all rocks and lavender and tufted grass,${c}| on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
  }

  fun `test visual line replace`() {
    val text = """
            I fo${c}und it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    VimPlugin.getRegister().storeText(myFixture.editor, text rangeOf "legendary", SelectionType.CHARACTER_WISE, false)
    typeText(parseKeys("V", "gr"))
    myFixture.checkResult("""
            ${c}legendary
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
  }

  fun `test visual line replace with line`() {
    val text = """
            I fo${c}und it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()

    configureByText(text)
    typeText(parseKeys("yyj", "V", "gr"))
    myFixture.checkResult("""
            I found it in a legendary land
            ${c}I found it in a legendary land
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent())
    assertMode(CommandState.Mode.COMMAND)
  }
}