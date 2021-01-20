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

class MotionInnerBlockTagActionTest : VimTestCase() {

  //|d| |v_it|
  fun testDeleteInnerTagBlockCaretInHtml() {
    typeTextInFile(parseKeys("dit"), "<template ${c}name=\"hello\">\n" +
      "  <button>Click Me</button>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n")
    myFixture.checkResult("<template name=\"hello\"></template>\n")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockCaretInHtmlUnclosedTag() {
    typeTextInFile(parseKeys("dit"), "<template ${c}name=\"hello\">\n" +
      "  <button>Click Me</button>\n" +
      "  <br>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n")
    myFixture.checkResult("<template name=\"hello\"></template>\n")
  }

  fun testDeleteInnerTagBlockCaretEdgeTag() {
    typeTextInFile(parseKeys("dit"), "<template name=\"hello\"${c}>\n" +
      "  <button>Click Me</button>\n" +
      "  <br>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n")
    myFixture.checkResult("<template name=\"hello\"></template>\n")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBefore() {
    typeTextInFile(parseKeys("dit"), "abc${c}de<tag>fg</tag>hi")
    myFixture.checkResult("abcde<tag>fg</tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInOpen() {
    typeTextInFile(parseKeys("dit"), "abcde<ta${c}g>fg</tag>hi")
    myFixture.checkResult("abcde<tag></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInOpenEndOfLine() {
    typeTextInFile(parseKeys("dit"), "abcde<ta${c}g>fg</tag>")
    myFixture.checkResult("abcde<tag></tag>")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInOpenStartOfLine() {
    typeTextInFile(parseKeys("dit"), "<ta${c}g>fg</tag>hi")
    myFixture.checkResult("<tag></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInOpenWithArgs() {
    typeTextInFile(parseKeys("dit"), "abcde<ta${c}g name = \"name\">fg</tag>hi")
    myFixture.checkResult("abcde<tag name = \"name\"></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetween() {
    typeTextInFile(parseKeys("dit"), "abcde<tag>f${c}g</tag>hi")
    myFixture.checkResult("abcde<tag></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenTagWithRegex() {
    typeTextInFile(parseKeys("dit"), "abcde<[abc]*>af${c}gbc</[abc]*>hi")
    myFixture.checkResult("abcde<[abc]*></[abc]*>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenCamelCase() {
    typeTextInFile(parseKeys("dit"), "abcde<tAg>f${c}g</tag>hi")
    myFixture.checkResult("abcde<tAg></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenCaps() {
    typeTextInFile(parseKeys("dit"), "abcde<tag>f${c}g</TAG>hi")
    myFixture.checkResult("abcde<tag></TAG>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenWithSpaceBeforeTag() {
    typeTextInFile(parseKeys("dit"), "abcde< tag>f${c}g</ tag>hi")
    myFixture.checkResult("abcde< tag>fg</ tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenWithSpaceAfterTag() {
    typeTextInFile(parseKeys("dit"), "abcde<tag >f${c}g</tag>hi")
    myFixture.checkResult("abcde<tag ></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenWithArgs() {
    typeTextInFile(parseKeys("dit"), "abcde<tag name = \"name\">f${c}g</tag>hi")
    myFixture.checkResult("abcde<tag name = \"name\"></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInClose() {
    typeTextInFile(parseKeys("dit"), "abcde<tag>fg</ta${c}g>hi")
    myFixture.checkResult("abcde<tag></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockAfter() {
    typeTextInFile(parseKeys("dit"), "abcde<tag>fg</tag>h${c}i")
    myFixture.checkResult("abcde<tag>fg</tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInAlone() {
    typeTextInFile(parseKeys("dit"), "abcde<ta${c}g>fghi")
    myFixture.checkResult("abcde<tag>fghi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockWithoutTags() {
    typeTextInFile(parseKeys("dit"), "abc${c}de")
    myFixture.checkResult("abcde")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBeforeWithoutOpenTag() {
    typeTextInFile(parseKeys("dit"), "abc${c}defg</tag>hi")
    myFixture.checkResult("abcdefg</tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInCloseWithoutOpenTag() {
    typeTextInFile(parseKeys("dit"), "abcdefg</ta${c}g>hi")
    myFixture.checkResult("abcdefg</tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockAfterWithoutOpenTag() {
    typeTextInFile(parseKeys("dit"), "abcdefg</tag>h${c}i")
    myFixture.checkResult("abcdefg</tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBeforeWithoutCloseTag() {
    typeTextInFile(parseKeys("dit"), "abc${c}defg<tag>hi")
    myFixture.checkResult("abcdefg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInOpenWithoutCloseTag() {
    typeTextInFile(parseKeys("dit"), "abcdefg<ta${c}g>hi")
    myFixture.checkResult("abcdefg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockAfterWithoutCloseTag() {
    typeTextInFile(parseKeys("dit"), "abcdefg<tag>h${c}i")
    myFixture.checkResult("abcdefg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBeforeWrongOrder() {
    typeTextInFile(parseKeys("dit"), "abc${c}de</tag>fg<tag>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInOpenWrongOrder() {
    typeTextInFile(parseKeys("dit"), "abcde</ta${c}g>fg<tag>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBetweenWrongOrder() {
    typeTextInFile(parseKeys("dit"), "abcde</tag>f${c}g<tag>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockInCloseWrongOrder() {
    typeTextInFile(parseKeys("dit"), "abcde</tag>fg<ta${c}g>hi")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockTwoTagsWrongOrder() {
    typeTextInFile(parseKeys("dit"), "<foo><html>t${c}ext</foo></html>")
    myFixture.checkResult("<foo></foo></html>")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockTwoTagsWrongOrderInClosingTag() {
    typeTextInFile(parseKeys("dit"), "<foo><html>text</foo></htm${c}l>")
    myFixture.checkResult("<foo><html></html>")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockAfterWrongOrder() {
    typeTextInFile(parseKeys("dit"), "abcde</tag>fg<tag>h${c}i")
    myFixture.checkResult("abcde</tag>fg<tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBracketInside() {
    typeTextInFile(parseKeys("dit"), "abcde<tag>f${c}<>g</tag>hi")
    myFixture.checkResult("abcde<tag></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagBlockBracketInsideString() {
    typeTextInFile(parseKeys("dit"), "abcde<tag>f${c}\"<>\"g</tag>hi")
    myFixture.checkResult("abcde<tag></tag>hi")
  }

  //|d| |v_it|
  fun testDeleteInnerTagIsCaseInsensitive() {
    typeTextInFile(parseKeys("dit"), "<a> <as${c}df> </A>")
    myFixture.checkResult("<a></A>")
  }

  //|d| |v_it|
  fun testDeleteInnerTagSlashesInAttribute() {
    typeTextInFile(parseKeys("dit"), "<a href=\"https://isitchristmas.com\" class=\"button\">Bing ${c}Bing bing</a>")
    myFixture.checkResult("<a href=\"https://isitchristmas.com\" class=\"button\"></a>")
  }

  // VIM-1090 |d| |v_it|
  // Adapted from vim source file "test_textobjects.vim"
  fun testDeleteInnerTagDuplicateTags() {
    typeTextInFile(parseKeys("dit"), "<b>as${c}d<i>as<b />df</i>asdf</b>")
    myFixture.checkResult("<b></b>")
  }

  // |v_it|
  fun testFileStartsWithSlash() {
    configureByText("/*hello\n" +
      "${c}foo\n" +
      "bar>baz\n")
    typeText(parseKeys("vit"))
    assertPluginError(true)
  }

  // |v_it|
  fun testSelectInnerTagEmptyTag() {
    configureByText("<a>${c}</a>")
    typeText(parseKeys("vit"))
    assertSelection("<a></a>")
  }

  fun `test single character`() {
    // The whole tag block is also selected if there is only a single character inside
    configureByText("<a>${c}a</a>")
    typeText(parseKeys("vit"))
    assertSelection("<a>a</a>")
  }

  fun `test single character inside tag`() {
    configureByText("<a${c}></a>")
    typeText(parseKeys("vit"))
    assertSelection("<")
  }

  // VIM-1633 |v_it|
  fun testNestedInTagSelection() {
    configureByText("<t>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vit"))
    assertSelection("Inner")
  }

  fun `test nested tag double motion`() {
    configureByText("<o>Outer\n" +
      " ${c}  <t></t>\n" +
      "</o>\n")
    typeText(parseKeys("vitit"))
    assertSelection("<t></t>")
  }

  fun `test in inner tag double motion`() {
    configureByText("<o><t>${c}</t>\n</o>")
    typeText(parseKeys("vitit"))
    assertSelection("<o><t></t>\n</o>")
  }

  fun `test nested tags between tags`() {
    configureByText("<t>Outer\n" +
      "   <t>Inner</t> ${c} <t>Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>  <t>Inner</t>")
  }

  fun `test nested tags number motion`() {
    configureByText("<t>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("v2it"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags double motion`() {
    configureByText("<o>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</o>\n")
    typeText(parseKeys("vitit"))
    assertSelection("<t>Inner</t>")
  }

  fun `test nested tags triple motion`() {
    configureByText("<t>Outer\n" +
      "   <t>${c}Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vititit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags in closing tag`() {
    configureByText("<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</${c}t>\n")
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags in opening tag`() {
    configureByText("<${c}t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags ouside tag`() {
    configureByText("${c}<t>Outer\n" +
      "   <t>Inner</t>\n" +
      "</t>\n")
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test skip whitespace at start of line`() {
    configureByText("<o>Outer\n" +
      " ${c}  <t></t>\n" +
      "</o>\n")
    typeText(parseKeys("vit"))
    assertSelection("<")
  }
}
