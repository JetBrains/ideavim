/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionOuterBlockTagActionTest : VimTestCase() {

  // |d| |v_at|
  fun testDeleteOuterTagBlockBefore() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}de<tag>fg</tag>hi")
    assertState("abcde<tag>fg</tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInOpen() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<ta${c}g>fg</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInOpenWithArgs() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<ta${c}g name = \"name\">fg</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockBetween() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag>f${c}g</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockBetweenWithArgs() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag name = \"name\">f${c}g</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInClose() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag>fg</ta${c}g>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockAfter() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag>fg</tag>h${c}i")
    assertState("abcde<tag>fg</tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInAlone() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<ta${c}g>fghi")
    assertState("abcde<tag>fghi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockWithoutTags() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}de")
    assertState("abcde")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockBeforeWithoutOpenTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}defg</tag>hi")
    assertState("abcdefg</tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInCloseWithoutOpenTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg</ta${c}g>hi")
    assertState("abcdefg</tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockAfterWithoutOpenTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg</tag>h${c}i")
    assertState("abcdefg</tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockBeforeWithoutCloseTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}defg<tag>hi")
    assertState("abcdefg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInOpenWithoutCloseTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg<ta${c}g>hi")
    assertState("abcdefg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockAfterWithoutCloseTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg<tag>h${c}i")
    assertState("abcdefg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockBeforeWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}de</tag>fg<tag>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInOpenWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</ta${c}g>fg<tag>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockBetweenWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</tag>f${c}g<tag>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockInCloseWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</tag>fg<ta${c}g>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  fun testDeleteOuterTagBlockAfterWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</tag>fg<tag>h${c}i")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_it|
  fun testDeleteInnerTagAngleBrackets() {
    typeTextInFile(injector.parser.parseKeys("dit"), "<div ${c}hello=\"d > hsj < akl\"></div>")
    assertState("<div hello=\"d ></div>")
  }

  // VIM-1090 |d| |v_at|
  fun testDeleteOuterTagDuplicateTags() {
    typeTextInFile(injector.parser.parseKeys("dat"), "<a><a></a></a$c>")
    assertState("")
  }

  // |v_it| |v_at|
  fun testTagSelectionSkipsWhitespaceAtStartOfLine() {
    // Also skip tabs
    configureByText(
      "<o>Outer\n" +
        " $c \t <t>Inner</t>\n" +
        "</o>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection("<t>Inner</t>")
  }

  fun `test skip new line`() {
    // Newline must not be skipped
    configureByText("${c}\n" + "    <t>asdf</t>")
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(null)
  }

  fun `test whitespace skip`() {
    // Whitespace is only skipped if there is nothing else at the start of the line
    configureByText(
      "<o>Outer\n" +
        "a $c  <t>Inner</t>\n" +
        "</o>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<o>Outer\n" +
        "a   <t>Inner</t>\n" +
        "</o>"
    )
  }

  // |v_at|
  fun testNestedTagSelection() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection("<t>Inner</t>")
  }

  fun `test nested tags between tags`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t> $c <t>Inner</t>\n" +
        "</t>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>  <t>Inner</t>\n" +
        "</t>"
    )
  }

  fun `test nested tags double motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n"
    )
    typeText(injector.parser.parseKeys("vatat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>"
    )
  }

  fun `test nested tags number motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n"
    )
    typeText(injector.parser.parseKeys("v2at"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>"
    )
  }

  fun `test nested tags on outer`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</${c}t>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>"
    )
  }

  fun `test nested tags on outer start`() {
    configureByText(
      "<${c}t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>"
    )
  }

  fun `test nested tags outside outer`() {
    configureByText(
      "$c<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n"
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>"
    )
  }
}
