/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.ide.CopyPasteManager
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.datatransfer.StringSelection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestWithoutNeovim(SkipNeovimReason.OPTION)
class VirtualEditOptionTest : VimTestCase() {
  private val text = """
    ${c}line one
    second line here
    ab
  """.trimIndent()

  private fun setUpWith(ve: String, content: String = text) {
    configureByText(content)
    enterCommand("set virtualedit=$ve")
  }

  @Test
  fun `test l moves past end of line with ve=all`() {
    setUpWith("all")
    typeText("$5l")
    assertPosition(0, 12)
  }

  @Test
  fun `test l stays on last character with ve=insert`() {
    setUpWith("insert")
    typeText("$5l")
    assertPosition(0, 7)
  }

  @Test
  fun `test h moves back from virtual space`() {
    setUpWith("all")
    typeText("$5l3h")
    assertPosition(0, 9)
  }

  @Test
  fun `test h moves back from virtual space into text`() {
    setUpWith("all")
    typeText("$5l6h")
    assertPosition(0, 6)
  }

  @Test
  fun `test column motion past end of line`() {
    setUpWith("all")
    typeText("20|")
    assertPosition(0, 19)
  }

  @Test
  fun `test j keeps column past end of short line`() {
    setUpWith("all")
    typeText("5ljj")
    assertPosition(2, 5)
  }

  @Test
  fun `test j after dollar keeps column of last character`() {
    setUpWith("all")
    typeText("\$j")
    assertPosition(1, 7)
  }

  @Test
  fun `test j after dollar goes to virtual space on short line`() {
    setUpWith("all", "long line number one here\nab")
    typeText("\$j")
    assertPosition(1, 24)
  }

  @Test
  fun `test insert in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5lix<Esc>")
    assertState(
      """
      line one    ${c}x
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test append in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5la!<Esc>")
    assertState(
      """
      line one     ${c}!
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test escape from insert in virtual space moves one column left`() {
    setUpWith("all")
    typeText("$5li<Esc>")
    assertPosition(0, 11)
    assertState(text.replace(c, ""))
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test arrow right in insert mode moves past end of line with ve=insert`() {
    setUpWith("insert")
    typeText("A<Right><Right>y<Esc>")
    assertState(
      """
      line one  ${c}y
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test leaving insert mode with ve=insert returns caret to text`() {
    setUpWith("insert")
    typeText("A<Right><Right><Esc>")
    assertPosition(0, 7)
    assertFalse(fixture.editor.settings.isVirtualSpace)
  }

  @Test
  fun `test editor virtual space follows mode with ve=insert`() {
    setUpWith("insert")
    assertFalse(fixture.editor.settings.isVirtualSpace)
    typeText("i")
    assertTrue(fixture.editor.settings.isVirtualSpace)
    typeText("<Esc>")
    assertFalse(fixture.editor.settings.isVirtualSpace)
  }

  @Test
  fun `test editor virtual space is restored when ve is reset`() {
    setUpWith("all")
    assertTrue(fixture.editor.settings.isVirtualSpace)
    typeText("$5l")
    enterCommand("set virtualedit=")
    assertFalse(fixture.editor.settings.isVirtualSpace)
    assertPosition(0, 7)
  }

  @Test
  fun `test empty ve does not touch IDE virtual space`() {
    configureByText(text)
    ApplicationManager.getApplication().invokeAndWait { fixture.editor.settings.isVirtualSpace = true }
    enterCommand("set virtualedit=onemore")
    typeText("i")
    typeText("<Esc>")
    assertTrue(fixture.editor.settings.isVirtualSpace)
  }

  @Test
  fun `test x in virtual space does nothing`() {
    setUpWith("all")
    typeText("$5lx")
    assertPosition(0, 12)
    assertState(text.replace(c, ""))
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test D in virtual space does nothing`() {
    setUpWith("all")
    typeText("$5lD")
    assertPosition(0, 12)
    assertState(text.replace(c, ""))
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test replace character in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("G5lrZ")
    assertState(
      """
      line one
      second line here
      ab   ${c}Z
      """.trimIndent(),
    )
  }

  @Test
  fun `test put in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("yiw$5lp")
    assertState(
      """
      line one     lin${c}e
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test put before in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("yiw$5lP")
    assertState(
      """
      line one    lin${c}e
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test repeat insert in virtual space`() {
    setUpWith("all")
    typeText("G5lix<Esc>j.")
    assertState(
      """
      line one
      second line here
      ab   ${c}xx
      """.trimIndent(),
    )
  }

  @Test
  fun `test X in virtual space moves one column left`() {
    setUpWith("all")
    typeText("$5lX")
    assertPosition(0, 11)
    assertState(text.replace(c, ""))
  }

  @Test
  fun `test substitute in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5ls!<Esc>")
    assertState(
      """
      line one    ${c}!
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test replace mode in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5lR!!<Esc>")
    assertState(
      """
      line one    !${c}!
      second line here
      ab
      """.trimIndent(),
    )
  }

  @Test
  fun `test put on empty line pads with spaces`() {
    setUpWith("all", "line one\n\nab")
    typeText("yiwjp")
    assertState("line one\n lin${c}e\nab")
  }

  @Test
  fun `test caret stays in virtual space after IDE action`() {
    setUpWith("all")
    typeText("$5l")
    typeText("<Action>(EditorLineEnd)")
    assertPosition(0, 8)
  }

  @Test
  fun `test visual delete in virtual space keeps new line`() {
    setUpWith("all")
    typeText("$5lvd")
    assertState(text.replace(c, ""))
  }

  @Test
  fun `test visual delete back from virtual space keeps new line`() {
    setUpWith("all")
    typeText("$5lv0d")
    assertState("\nsecond line here\nab")
  }

  @Test
  fun `test visual delete from virtual space to next line joins lines`() {
    setUpWith("all")
    typeText("$5lvjd")
    assertState("line oneere\nab")
  }

  @Test
  fun `test visual delete to line end keeps new line`() {
    setUpWith("all")
    typeText("v\$d")
    assertState("\nsecond line here\nab")
  }

  @Test
  fun `test visual change from text to virtual space keeps new line`() {
    setUpWith("all")
    typeText("lv10lc!<Esc>")
    assertState("l!\nsecond line here\nab")
  }

  @Test
  fun `test delete to line end in virtual space does nothing`() {
    setUpWith("all")
    typeText("$5ld$")
    assertState(text.replace(c, ""))
  }

  @Test
  fun `test change to line end in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5lc\$X<Esc>")
    assertState("line one    X\nsecond line here\nab")
  }

  @Test
  fun `test C in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5lC!<Esc>")
    assertState("line one    !\nsecond line here\nab")
  }

  @Test
  fun `test cl in virtual space pads with spaces`() {
    setUpWith("all")
    typeText("$5lcl!<Esc>")
    assertState("line one    !\nsecond line here\nab")
  }

  @Test
  fun `test single command from insert mode in virtual space`() {
    setUpWith("all")
    typeText("$5li<C-O>x<Esc>")
    assertState(text.replace(c, ""))
  }

  @Test
  fun `test append with count in virtual space pads once`() {
    setUpWith("all")
    typeText("$5l3a!<Esc>")
    assertState("line one     !!!\nsecond line here\nab")
  }

  @Test
  fun `test insert with count in virtual space pads once`() {
    setUpWith("all")
    typeText("$5l3i!<Esc>")
    assertState("line one    !!!\nsecond line here\nab")
  }

  @Test
  fun `test X at end of line deletes last character`() {
    setUpWith("all")
    typeText("\$lX")
    assertState("line on\nsecond line here\nab")
  }

  @Test
  fun `test l does not wrap with whichwrap`() {
    setUpWith("all")
    enterCommand("set whichwrap=b,s,h,l")
    typeText("\$ll")
    assertPosition(0, 9)
  }

  @Test
  fun `test visual block from virtual space works on text`() {
    setUpWith("all")
    typeText("$5l<C-V>jd")
    assertState("line on\nsecond ine here\nab")
  }

  @Test
  fun `test paste with count in virtual space pads once`() {
    setUpWith("all")
    ApplicationManager.getApplication().invokeAndWait {
      CopyPasteManager.getInstance().setContents(StringSelection("P"))
    }
    typeText("$5l3a<Action>(EditorPaste)<Esc>")
    assertState("line one     PPP\nsecond line here\nab")
  }
  @Test
  fun `test backspace in virtual space is not repeated`() {
    setUpWith("all", "alpha bravo\nab\n\nlonger line here")
    typeText("j6li<BS>X<Esc>jj6l.")
    assertEquals("alpha bravo\nab   X\n\nlonger lineX here", fixture.editor.document.text)
  }

  @Test
  fun `test exiting visual mode in virtual space keeps column`() {
    setUpWith("all")
    typeText("$5lv<Esc>v<Esc>")
    assertPosition(0, 12)
  }

  @Test
  fun `test single command from insert mode keeps virtual column with ve=insert`() {
    setUpWith("insert")
    typeText("A<Right><Right><C-O>zzX<Esc>")
    assertEquals("line one  X\nsecond line here\nab", fixture.editor.document.text)
  }

  @Test
  fun `test editor follows IDE virtual space setting after ve is reset`() {
    setUpWith("all")
    enterCommand("set virtualedit=")
    val ideSettings = EditorSettingsExternalizable.getInstance()
    try {
      ApplicationManager.getApplication().invokeAndWait { ideSettings.isVirtualSpace = true }
      assertTrue(fixture.editor.settings.isVirtualSpace)
    } finally {
      ApplicationManager.getApplication().invokeAndWait { ideSettings.isVirtualSpace = false }
    }
  }
  @Test
  fun `test delete word in virtual space moves to line end`() {
    setUpWith("all", "alpha bravo\nab\n\nlonger line here")
    typeText("j6li<C-W>X<Esc>")
    assertEquals("alpha bravo\nabX\n\nlonger line here", fixture.editor.document.text)
  }

  @Test
  fun `test delete word in virtual space is not repeated`() {
    setUpWith("all", "alpha bravo\nab\n\nlonger line here")
    typeText("j6li<C-W>X<Esc>jj6l.")
    assertEquals("alpha bravo\nabX\n\nlonger lXine here", fixture.editor.document.text)
  }

  @Test
  fun `test delete inserted text in virtual space deletes line`() {
    setUpWith("all", "alpha bravo\nab\n\nlonger line here")
    typeText("j6li<C-U>X<Esc>")
    assertEquals("alpha bravo\nX\n\nlonger line here", fixture.editor.document.text)
  }

  @Test
  fun `test replace character in virtual space after tab pads by columns`() {
    setUpWith("all", "\tab")
    typeText("$5lrZ")
    assertEquals("\tab    Z", fixture.editor.document.text)
  }
  @Test
  fun `test backspace with one of carets in virtual space`() {
    setUpWith("all", "${c}ab\n${c}longer line here")
    typeText("4li<BS>X<Esc>")
    assertEquals("ab X\nlonXer line here", fixture.editor.document.text)
  }

  @Test
  fun `test backspace in virtual space with primary caret in text is repeated`() {
    setUpWith("all", "${c}ab\n${c}longer line here\nlonger line here")
    typeText("4li<BS>X<Esc><Esc>Gl.")
    assertEquals("ab X\nlonXer line here\nXonger line here", fixture.editor.document.text)
  }
  @Test
  fun `test backspace in virtual space with primary caret in virtual space is not repeated`() {
    setUpWith("all", "${c}longer line here\n${c}ab\nlonger line here")
    typeText("4li<BS>X<Esc><Esc>G6l.")
    assertEquals("lonXer line here\nab X\nlongerX line here", fixture.editor.document.text)
  }

  @Test
  fun `test expression register at line end with ve=insert`() {
    setUpWith("insert")
    typeText("A<C-R>=1<CR><Esc>")
    assertEquals("line one1\nsecond line here\nab", fixture.editor.document.text)
  }

  @Test
  fun `test CTRL-backslash CTRL-N at line end with ve=insert`() {
    setUpWith("insert")
    typeText("A<C-\\><C-N>")
    assertPosition(0, 7)
  }

  @Test
  fun `test command line from single command keeps virtual column with ve=insert`() {
    setUpWith("insert")
    typeText("A<Right><Right><C-O>:<CR>X<Esc>")
    assertEquals("line one  X\nsecond line here\nab", fixture.editor.document.text)
  }

  @Test
  fun `test delete to End in virtual space does nothing`() {
    setUpWith("all")
    typeText("$5ld<End>")
    assertState(text.replace(c, ""))
  }

  @Test
  fun `test End then j keeps column of last character`() {
    setUpWith("all")
    typeText("$5l<End>j")
    assertPosition(1, 7)
  }

  @Test
  fun `test X with count from virtual space deletes text`() {
    setUpWith("all")
    typeText("$5l5X")
    assertEquals("line on\nsecond line here\nab", fixture.editor.document.text)
    assertPosition(0, 7)
  }

  @Test
  fun `test tilde in virtual space keeps caret`() {
    setUpWith("all")
    typeText("$5l~")
    assertState(text.replace(c, ""))
    assertPosition(0, 12)
  }

  @Test
  fun `test insert character above in virtual space pads with spaces`() {
    setUpWith("all", "long line one\nab")
    typeText("j5li<C-Y><Esc>")
    assertEquals("long line one\nab   l", fixture.editor.document.text)
  }

  @Test
  fun `test insert previous text in virtual space pads with spaces`() {
    setUpWith("all", "long line one\nab")
    typeText("ix<Esc>j5li<C-A><Esc>")
    assertEquals("xlong line one\nab   x", fixture.editor.document.text)
  }

  @Test
  fun `test delete in virtual space joins lines`() {
    setUpWith("all", "ab\nnext")
    typeText("$3li<Del><Esc>")
    assertEquals("abnext", fixture.editor.document.text)
  }

  @Test
  fun `test j to column at line end of shorter line`() {
    setUpWith("all", "abc\ncd")
    typeText("3|j")
    assertPosition(1, 2)
  }

  @Test
  fun `test x at last character keeps caret at line end`() {
    setUpWith("all")
    typeText("\$x")
    assertEquals("line on\nsecond line here\nab", fixture.editor.document.text)
    assertPosition(0, 7)
  }

  @Test
  fun `test exiting visual mode at line end with ve=onemore`() {
    setUpWith("onemore", "the")
    typeText("\$v<Esc>")
    assertPosition(0, 2)
  }

  @Test
  fun `test insert in virtual space after tab pads by columns`() {
    setUpWith("all", "\tab")
    typeText("$5lix<Esc>")
    assertEquals("\tab    x", fixture.editor.document.text)
  }
}
