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

package org.jetbrains.plugins.ideavim.action.change.shift

import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.plugins.ideavim.VimTestCase

class ShiftRightTest : VimTestCase() {
  fun `test shift till new line`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(StringHelper.parseKeys(">W"), file)
    myFixture.checkResult("""
            A Discovery

                  I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent())
  }

  // VIM-407
  fun testShiftShiftsOneCharacterSingleLine() {
    myFixture.configureByText("a.txt", "<caret>w\n")
    typeText(StringHelper.parseKeys(">>"))
    myFixture.checkResult("    w\n")
  }

  // VIM-407
  fun testShiftShiftsOneCharacterMultiLine() {
    myFixture.configureByText("a.txt", "Hello\n<caret>w\nWorld")
    typeText(StringHelper.parseKeys(">>"))
    myFixture.checkResult("Hello\n    w\nWorld")
  }

  fun testShiftShiftsMultipleCharactersOneLine() {
    myFixture.configureByText("a.txt", "<caret>Hello, world!\n")
    typeText(StringHelper.parseKeys(">>"))
    myFixture.checkResult("    Hello, world!\n")
  }

  fun testShiftShiftsMultipleCharactersMultipleLines() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n")
    typeText(StringHelper.parseKeys("j>>"))
    myFixture.checkResult("Hello,\n    world!\n")
  }

  fun testShiftsSingleLineSelection() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n")
    typeText(StringHelper.parseKeys("jv$>>"))
    myFixture.checkResult("Hello,\n    world!\n")
  }

  fun testShiftsMultiLineSelection() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n")
    typeText(StringHelper.parseKeys("vj$>>"))
    myFixture.checkResult("    Hello,\n    world!\n")
  }

  fun testShiftsMultiLineSelectionSkipsNewline() {
    myFixture.configureByText("a.txt", "<caret>Hello,\nworld!\n\n")
    typeText(StringHelper.parseKeys("vG$>>"))
    myFixture.checkResult("    Hello,\n    world!\n\n")
  }

  fun testShiftsMultiLineSelectionSkipsNewlineWhenCursorNotInFirstColumn() {
    myFixture.configureByText("a.txt", "<caret>Hello,\n\nworld!\n")
    typeText(StringHelper.parseKeys("lVG>"))
    myFixture.checkResult("    Hello,\n\n    world!\n")
  }

  fun testShiftsMultiLineSelectionAddsTrailingWhitespaceIfTherePreviouslyWas() {
    myFixture.configureByText("a.txt", "<caret>Hello,\n    \nworld!\n")
    typeText(StringHelper.parseKeys("lVG>"))
    myFixture.checkResult("    Hello,\n        \n    world!\n")
  }

  // VIM-705 repeating a multiline indent would only affect last line
  fun testShiftsMultiLineSelectionRepeat() {
    myFixture.configureByText("a.txt", "<caret>a\nb\n")
    typeText(StringHelper.parseKeys("Vj>."))
    myFixture.checkResult("        a\n        b\n")
  }

  fun testShiftsDontCrashKeyHandler() {
    myFixture.configureByText("a.txt", "\n")
    typeText(StringHelper.parseKeys("<I<>", "<I<>"))
  }

  fun testShiftsVisualBlockMode() {
    myFixture.configureByText("a.txt", "foo<caret>foo\nfoobar\nfoobaz\n")
    typeText(StringHelper.parseKeys("<C-V>jjl>"))
    myFixture.checkResult("foo    foo\nfoo    bar\nfoo    baz\n")
  }

  fun `test shift ctrl-t`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(StringHelper.parseKeys("i<C-T>"), file)
    myFixture.checkResult("""
            A Discovery

                  I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent())
  }
}
