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

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionInnerBlockTagActionTest : VimTestCase() {

  // |d| |v_it|
  fun testDeleteInnerTagBlockCaretInHtml() {
    val keys = listOf("dit")
    val before = "<template ${c}name=\"hello\">\n" +
      "  <button>Click Me</button>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n"

    val after = "<template name=\"hello\"></template>\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockCaretInHtmlUnclosedTag() {
    val keys = listOf("dit")
    val before = "<template ${c}name=\"hello\">\n" +
      "  <button>Click Me</button>\n" +
      "  <br>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n"

    val after = "<template name=\"hello\"></template>\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteInnerTagBlockCaretEdgeTag() {
    val keys = listOf("dit")
    val before = "<template name=\"hello\"$c>\n" +
      "  <button>Click Me</button>\n" +
      "  <br>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n"

    val after = "<template name=\"hello\"></template>\n"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBefore() {
    val keys = listOf("dit")
    val before = "abc${c}de<tag>fg</tag>hi"
    val after = "abcde<tag>fg</tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInOpen() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g>fg</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInOpenEndOfLine() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g>fg</tag>"
    val after = "abcde<tag></tag>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInOpenStartOfLine() {
    val keys = listOf("dit")
    val before = "<ta${c}g>fg</tag>hi"
    val after = "<tag></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInOpenWithArgs() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g name = \"name\">fg</tag>hi"
    val after = "abcde<tag name = \"name\"></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetween() {
    val keys = listOf("dit")
    val before = "abcde<tag>f${c}g</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testDeleteInnerTagBlockBetweenTagWithRegex() {
    val keys = listOf("dit")
    val before = "abcde<[abc]*>af${c}gbc</[abc]*>hi"
    val after = "abcde<[abc]*></[abc]*>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetweenCamelCase() {
    val keys = listOf("dit")
    val before = "abcde<tAg>f${c}g</tag>hi"
    val after = "abcde<tAg></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetweenCaps() {
    val keys = listOf("dit")
    val before = "abcde<tag>f${c}g</TAG>hi"
    val after = "abcde<tag></TAG>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetweenWithSpaceBeforeTag() {
    val keys = listOf("dit")
    val before = "abcde< tag>f${c}g</ tag>hi"
    val after = "abcde< tag>fg</ tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetweenWithSpaceAfterTag() {
    val keys = listOf("dit")
    val before = "abcde<tag >f${c}g</tag>hi"
    val after = "abcde<tag ></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetweenWithArgs() {
    val keys = listOf("dit")
    val before = "abcde<tag name = \"name\">f${c}g</tag>hi"
    val after = "abcde<tag name = \"name\"></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInClose() {
    val keys = listOf("dit")
    val before = "abcde<tag>fg</ta${c}g>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockAfter() {
    val keys = listOf("dit")
    val before = "abcde<tag>fg</tag>h${c}i"
    val after = "abcde<tag>fg</tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInAlone() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g>fghi"
    val after = "abcde<tag>fghi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockWithoutTags() {
    val keys = listOf("dit")
    val before = "abc${c}de"
    val after = "abcde"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBeforeWithoutOpenTag() {
    val keys = listOf("dit")
    val before = "abc${c}defg</tag>hi"
    val after = "abcdefg</tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInCloseWithoutOpenTag() {
    val keys = listOf("dit")
    val before = "abcdefg</ta${c}g>hi"
    val after = "abcdefg</tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockAfterWithoutOpenTag() {
    val keys = listOf("dit")
    val before = "abcdefg</tag>h${c}i"
    val after = "abcdefg</tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBeforeWithoutCloseTag() {
    val keys = listOf("dit")
    val before = "abc${c}defg<tag>hi"
    val after = "abcdefg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInOpenWithoutCloseTag() {
    val keys = listOf("dit")
    val before = "abcdefg<ta${c}g>hi"
    val after = "abcdefg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockAfterWithoutCloseTag() {
    val keys = listOf("dit")
    val before = "abcdefg<tag>h${c}i"
    val after = "abcdefg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBeforeWrongOrder() {
    val keys = listOf("dit")
    val before = "abc${c}de</tag>fg<tag>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInOpenWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</ta${c}g>fg<tag>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBetweenWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</tag>f${c}g<tag>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockInCloseWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</tag>fg<ta${c}g>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testDeleteInnerTagBlockTwoTagsWrongOrder() {
    val keys = listOf("dit")
    val before = "<foo><html>t${c}ext</foo></html>"
    val after = "<foo></foo></html>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testDeleteInnerTagBlockTwoTagsWrongOrderInClosingTag() {
    val keys = listOf("dit")
    val before = "<foo><html>text</foo></htm${c}l>"
    val after = "<foo><html></html>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockAfterWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</tag>fg<tag>h${c}i"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBracketInside() {
    val keys = listOf("dit")
    val before = "abcde<tag>f$c<>g</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagBlockBracketInsideString() {
    val keys = listOf("dit")
    val before = "abcde<tag>f${c}\"<>\"g</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagIsCaseInsensitive() {
    val keys = listOf("dit")
    val before = "<a> <as${c}df> </A>"
    val after = "<a></A>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |d| |v_it|
  fun testDeleteInnerTagSlashesInAttribute() {
    val keys = listOf("dit")
    val before = "<a href=\"https://isitchristmas.com\" class=\"button\">Bing ${c}Bing bing</a>"
    val after = "<a href=\"https://isitchristmas.com\" class=\"button\"></a>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1090 |d| |v_it|
  // Adapted from vim source file "test_textobjects.vim"
  fun testDeleteInnerTagDuplicateTags() {
    val keys = listOf("dit")
    val before = "<b>as${c}d<i>as<b />df</i>asdf</b>"
    val after = "<b></b>"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // |v_it|
  fun testFileStartsWithSlash() {
    val before = """
      /*hello
      ${c}foo
      bar>baz
      
      """.trimIndent()
    val after = """
      /*hello
      ${s}${c}f${se}oo
      bar>baz
      
      """.trimIndent()
    val keys = listOf("vit")
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
    assertPluginError(true)
  }

  // |v_it|
  fun testSelectInnerTagEmptyTag() {
    val before = "<a>$c</a>"
    val after = "${s}<a></a$c>${se}"
    configureByText(before)
    val keys = listOf("vit")
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  fun `test single character`() {
    // The whole tag block is also selected if there is only a single character inside
    val before = "<a>${c}a</a>"
    val after = "${s}<a>a</a${c}>${se}"
    val keys = listOf("vit")
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
  }

  fun `test single character inside tag`() {
    configureByText("<a$c></a>")
    typeText(parseKeys("vit"))
    assertSelection("<")
  }

  // VIM-1633 |v_it|
  fun testNestedInTagSelection() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n"
    )
    typeText(parseKeys("vit"))
    assertSelection("Inner")
  }

  fun `test nested tag double motion`() {
    configureByText(
      "<o>Outer\n" +
        " $c  <t></t>\n" +
        "</o>\n"
    )
    typeText(parseKeys("vitit"))
    assertSelection("<t></t>")
  }

  fun `test in inner tag double motion`() {
    configureByText("<o><t>$c</t>\n</o>")
    typeText(parseKeys("vitit"))
    assertSelection("<o><t></t>\n</o>")
  }

  fun `test nested tags between tags`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t> $c <t>Inner</t>\n" +
        "</t>\n"
    )
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>  <t>Inner</t>")
  }

  fun `test nested tags number motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n"
    )
    typeText(parseKeys("v2it"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags double motion`() {
    configureByText(
      "<o>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</o>\n"
    )
    typeText(parseKeys("vitit"))
    assertSelection("<t>Inner</t>")
  }

  fun `test nested tags triple motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n"
    )
    typeText(parseKeys("vititit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags in closing tag`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</${c}t>\n"
    )
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags in opening tag`() {
    configureByText(
      "<${c}t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n"
    )
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test nested tags ouside tag`() {
    configureByText(
      "$c<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n"
    )
    typeText(parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  fun `test skip whitespace at start of line`() {
    configureByText(
      "<o>Outer\n" +
        " $c  <t></t>\n" +
        "</o>\n"
    )
    typeText(parseKeys("vit"))
    assertSelection("<")
  }
}
