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

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionOuterBlockTagActionTest : VimTestCase() {

  //|d| |v_at|
  fun testDeleteOuterTagBlockBefore() {
    typeTextInFile(parseKeys("dat"), "abc${c}de<tag>fg</tag>hi")
    myFixture.checkResult("abcde<tag>fg</tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInOpen() {
    typeTextInFile(parseKeys("dat"), "abcde<ta${c}g>fg</tag>hi")
    myFixture.checkResult("abcdehi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInOpenWithArgs() {
    typeTextInFile(parseKeys("dat"), "abcde<ta${c}g name = \"name\">fg</tag>hi")
    myFixture.checkResult("abcdehi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockBetween() {
    typeTextInFile(parseKeys("dat"), "abcde<tag>f${c}g</tag>hi")
    myFixture.checkResult("abcdehi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockBetweenWithArgs() {
    typeTextInFile(parseKeys("dat"), "abcde<tag name = \"name\">f${c}g</tag>hi")
    myFixture.checkResult("abcdehi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInClose() {
    typeTextInFile(parseKeys("dat"), "abcde<tag>fg</ta${c}g>hi")
    myFixture.checkResult("abcdehi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockAfter() {
    typeTextInFile(parseKeys("dat"), "abcde<tag>fg</tag>h${c}i")
    myFixture.checkResult("abcde<tag>fg</tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInAlone() {
    typeTextInFile(parseKeys("dat"), "abcde<ta${c}g>fghi")
    myFixture.checkResult("abcde<tag>fghi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockWithoutTags() {
    typeTextInFile(parseKeys("dat"), "abc${c}de")
    myFixture.checkResult("abcde")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockBeforeWithoutOpenTag() {
    typeTextInFile(parseKeys("dat"), "abc${c}defg</tag>hi")
    myFixture.checkResult("abcdefg</tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInCloseWithoutOpenTag() {
    typeTextInFile(parseKeys("dat"), "abcdefg</ta${c}g>hi")
    myFixture.checkResult("abcdefg</tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockAfterWithoutOpenTag() {
    typeTextInFile(parseKeys("dat"), "abcdefg</tag>h${c}i")
    myFixture.checkResult("abcdefg</tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockBeforeWithoutCloseTag() {
    typeTextInFile(parseKeys("dat"), "abc${c}defg<tag>hi")
    myFixture.checkResult("abcdefg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInOpenWithoutCloseTag() {
    typeTextInFile(parseKeys("dat"), "abcdefg<ta${c}g>hi")
    myFixture.checkResult("abcdefg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockAfterWithoutCloseTag() {
    typeTextInFile(parseKeys("dat"), "abcdefg<tag>h${c}i")
    myFixture.checkResult("abcdefg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockBeforeWrongOrder() {
    typeTextInFile(parseKeys("dat"), "abc${c}de</tag>fg<tag>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInOpenWrongOrder() {
    typeTextInFile(parseKeys("dat"), "abcde</ta${c}g>fg<tag>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockBetweenWrongOrder() {
    typeTextInFile(parseKeys("dat"), "abcde</tag>f${c}g<tag>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockInCloseWrongOrder() {
    typeTextInFile(parseKeys("dat"), "abcde</tag>fg<ta${c}g>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_at|
  fun testDeleteOuterTagBlockAfterWrongOrder() {
    typeTextInFile(parseKeys("dat"), "abcde</tag>fg<tag>h${c}i")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagAngleBrackets() {
    typeTextInFile(parseKeys("dit"), "<div ${c}hello=\"d > hsj < akl\"></div>")
    myFixture.checkResult("<div hello=\"d ></div>")
  }

  // VIM-1090 |d| |v_at|
  fun testDeleteOuterTagDuplicateTags() {
    typeTextInFile(parseKeys("dat"), "<a><a></a></a${c}>")
    myFixture.checkResult("")
  }

  // |v_it| |v_at|
  fun testTagSelectionSkipsWhitespaceAtStartOfLine() {
    // Also skip tabs
    configureByText("<o>Outer\n" +
      " ${c} \t <t>Inner</t>\n" +
      "</o>\n")
    typeText(parseKeys("vat"))
    assertSelection("<t>Inner</t>")
  }

  fun `test skip new line`() {
    // Newline must not be skipped
    configureByText("${c}\n" + "    <t>asdf</t>")
    typeText(parseKeys("vat"))
    assertSelection(null)
  }

  fun `test whitespace skip`() {
    // Whitespace is only skipped if there is nothing else at the start of the line
    configureByText("<o>Outer\n" +
      "a ${c}  <t>Inner</t>\n" +
      "</o>\n")
    typeText(parseKeys("vat"))
    assertSelection("<o>Outer\n" +
      "a   <t>Inner</t>\n" +
      "</o>")
  }

  // |v_at|
  fun testNestedTagSelection() {
    configureByText("<t>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vat"))
    assertSelection("<t>Inner</t>")
  }

  fun `test nested tags between tags`() {
    configureByText("<t>Outer\n" +
      "   <t>Inner</t> ${c} <t>Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vat"))
    assertSelection("<t>Outer\n" +
      "   <t>Inner</t>  <t>Inner</t>\n" +
      "</t>")
  }

  fun `test nested tags double motion`() {
    configureByText("<t>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vatat"))
    assertSelection("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>")
  }

  fun `test nested tags number motion`() {
    configureByText("<t>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("v2at"))
    assertSelection("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>")
  }

  fun `test nested tags on outer`() {
    configureByText("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</${c}t>\n")
    typeText(parseKeys("vat"))
    assertSelection("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>")
  }

  fun `test nested tags on outer start`() {
    configureByText("<${c}t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vat"))
    assertSelection("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>")
  }

  fun `test nested tags outside outer`() {
    configureByText("${c}<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vat"))
    assertSelection("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>")
  }
}
