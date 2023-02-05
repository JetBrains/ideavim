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
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_INSERTED_TEXT_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.SMALL_DELETION_REGISTER
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.Assert

class SpecialRegistersTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    val registerGroup = VimPlugin.getRegister()
    registerGroup.setKeys('a', injector.parser.stringToKeys(DUMMY_TEXT))
    registerGroup.setKeys(SMALL_DELETION_REGISTER, injector.parser.stringToKeys(DUMMY_TEXT))
    run {
      var c = '0'
      while (c <= '9') {
        registerGroup.setKeys(c, injector.parser.stringToKeys(DUMMY_TEXT))
        c++
      }
    }
  }

  // VIM-581
  fun testSmallDelete() {
    typeTextInFile(injector.parser.parseKeys("de"), "one <caret>two three\n")
    assertEquals("two", getRegisterText(SMALL_DELETION_REGISTER))
    // Text smaller than line doesn't go to numbered registers (except special cases)
    assertRegisterNotChanged('1')
  }

  // |d| |%| Special case for small delete
  fun testSmallDeleteWithPercent() {
    typeTextInFile(injector.parser.parseKeys("d%"), "(one<caret> two) three\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |(| Special case for small delete
  fun testSmallDeleteTillPrevSentence() {
    typeTextInFile(injector.parser.parseKeys("d("), "One. Two<caret>. Three.\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |)| Special case for small delete
  fun testSmallDeleteTillNextSentence() {
    typeTextInFile(injector.parser.parseKeys("d)"), "One. <caret>Two. Three.\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |`| Special case for small delete
  fun testSmallDeleteWithMark() {
    typeTextInFile(injector.parser.parseKeys("ma" + "b" + "d`a"), "one two<caret> three\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |/| Special case for small delete
  fun testSmallDeleteWithSearch() {
    typeTextInFile(injector.parser.parseKeys("d/" + "o" + "<Enter>"), "one <caret>two three\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |?| Special case for small delete
  fun testSmallDeleteWithBackSearch() {
    typeTextInFile(injector.parser.parseKeys("d?" + "t" + "<Enter>"), "one two<caret> three\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |n| Special case for small delete
  fun testSmallDeleteWithSearchRepeat() {
    typeTextInFile(injector.parser.parseKeys("/" + "t" + "<Enter>" + "dn"), "<caret>one two three\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |N| Special case for small delete
  fun testSmallDeleteWithBackSearchRepeat() {
    typeTextInFile(injector.parser.parseKeys("/" + "t" + "<Enter>" + "dN"), "one tw<caret>o three\n")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |{| Special case for small delete
  fun testSmallDeleteTillPrevParagraph() {
    typeTextInFile(injector.parser.parseKeys("d{"), "one<caret> two three")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  // |d| |}| Special case for small delete
  fun testSmallDeleteTillNextParagraph() {
    typeTextInFile(injector.parser.parseKeys("d}"), "one<caret> two three")
    assertRegisterChanged('1')
    assertRegisterChanged(SMALL_DELETION_REGISTER)
  }

  fun testSmallDeleteInRegister() {
    typeTextInFile(injector.parser.parseKeys("\"ade"), "one <caret>two three\n")

    // Small deletes (less than a line) with register specified go to that register and to numbered registers
    assertRegisterChanged('a')
    assertRegisterNotChanged('1')
    assertRegisterNotChanged(SMALL_DELETION_REGISTER)
  }

  fun testLineDelete() {
    typeTextInFile(injector.parser.parseKeys("dd"), "one <caret>two three\n")
    assertRegisterChanged('1')
    assertRegisterNotChanged(SMALL_DELETION_REGISTER)
  }

  fun testLineDeleteInRegister() {
    typeTextInFile(injector.parser.parseKeys("\"add"), "one <caret>two three\n")
    assertRegisterChanged('a')
    assertRegisterNotChanged('1')
  }

  fun testNumberedRegistersShifting() {
    configureByText("<caret>one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten\n")
    typeText(injector.parser.parseKeys("dd" + "dd"))
    assertEquals("one\n", getRegisterText('2'))
    assertEquals("two\n", getRegisterText('1'))
    typeText(injector.parser.parseKeys("dd" + "dd" + "dd"))
    assertEquals("one\n", getRegisterText('5'))
    assertEquals("four\n", getRegisterText('2'))
    typeText(injector.parser.parseKeys("dd" + "dd" + "dd" + "dd"))
    assertEquals("one\n", getRegisterText('9'))
  }

  fun testSearchRegisterAfterSearch() {
    configureByText("<caret>one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten\n")
    enterSearch("three", true)
    assertEquals("three", getRegisterText(LAST_SEARCH_REGISTER))
  }

  fun testSearchRegisterAfterSubstitute() {
    configureByText("<caret>one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten\n")
    enterCommand("%s/three/3/g")
    assertEquals("three", getRegisterText(LAST_SEARCH_REGISTER))
  }

  fun testSearchRegisterAfterSearchRange() {
    configureByText("<caret>one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten\n")
    enterCommand("/three/d")
    assertEquals("three", getRegisterText(LAST_SEARCH_REGISTER))
  }

  fun testSearchRegisterAfterMultipleSearchRanges() {
    configureByText("<caret>one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine\nten\n")
    enterCommand("/one/;/three/d")
    assertEquals("three", getRegisterText(LAST_SEARCH_REGISTER))
  }

  fun testLastInsertedTextRegister() {
    configureByText("<caret>")
    typeText(injector.parser.parseKeys("i" + "abc" + "<Esc>"))
    assertEquals("abc", getRegisterText('.'))
    assertRegisterChanged(LAST_INSERTED_TEXT_REGISTER)
  }

  private fun assertRegisterChanged(registerName: Char) {
    val registerText = getRegisterText(registerName)
    Assert.assertNotEquals(DUMMY_TEXT, registerText)
  }

  private fun assertRegisterNotChanged(registerName: Char) {
    val registerText = getRegisterText(registerName)
    assertEquals(DUMMY_TEXT, registerText)
  }

  private fun getRegisterText(registerName: Char): String? {
    val registerGroup = VimPlugin.getRegister()
    val register = registerGroup.getRegister(registerName)
    assertNotNull(register)
    return register!!.text
  }

  companion object {
    private const val DUMMY_TEXT = "text"
  }
}
