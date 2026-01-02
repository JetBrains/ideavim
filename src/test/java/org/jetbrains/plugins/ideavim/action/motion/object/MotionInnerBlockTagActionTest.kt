/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionInnerBlockTagActionTest : VimTestCase() {

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockCaretInHtml() {
    val keys = listOf("dit")
    val before = "<template ${c}name=\"hello\">\n" +
      "  <button>Click Me</button>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n"

    val after = "<template name=\"hello\"></template>\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockCaretInHtmlUnclosedTag() {
    val keys = listOf("dit")
    val before = "<template ${c}name=\"hello\">\n" +
      "  <button>Click Me</button>\n" +
      "  <br>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n"

    val after = "<template name=\"hello\"></template>\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteInnerTagBlockCaretEdgeTag() {
    val keys = listOf("dit")
    val before = "<template name=\"hello\"$c>\n" +
      "  <button>Click Me</button>\n" +
      "  <br>\n" +
      "  <p>You've pressed the button {{counter}} times.</p>\n" +
      "</template>\n"

    val after = "<template name=\"hello\"></template>\n"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBefore() {
    val keys = listOf("dit")
    val before = "abc${c}de<tag>fg</tag>hi"
    val after = "abcde<tag>fg</tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInOpen() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g>fg</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInOpenEndOfLine() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g>fg</tag>"
    val after = "abcde<tag></tag>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInOpenStartOfLine() {
    val keys = listOf("dit")
    val before = "<ta${c}g>fg</tag>hi"
    val after = "<tag></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInOpenWithArgs() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g name = \"name\">fg</tag>hi"
    val after = "abcde<tag name = \"name\"></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetween() {
    val keys = listOf("dit")
    val before = "abcde<tag>f${c}g</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testDeleteInnerTagBlockBetweenTagWithRegex() {
    val keys = listOf("dit")
    val before = "abcde<[abc]*>af${c}gbc</[abc]*>hi"
    val after = "abcde<[abc]*></[abc]*>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetweenCamelCase() {
    val keys = listOf("dit")
    val before = "abcde<tAg>f${c}g</tag>hi"
    val after = "abcde<tAg></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetweenCaps() {
    val keys = listOf("dit")
    val before = "abcde<tag>f${c}g</TAG>hi"
    val after = "abcde<tag></TAG>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetweenWithSpaceBeforeTag() {
    val keys = listOf("dit")
    val before = "abcde< tag>f${c}g</ tag>hi"
    val after = "abcde< tag>fg</ tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetweenWithSpaceAfterTag() {
    val keys = listOf("dit")
    val before = "abcde<tag >f${c}g</tag>hi"
    val after = "abcde<tag ></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetweenWithArgs() {
    val keys = listOf("dit")
    val before = "abcde<tag name = \"name\">f${c}g</tag>hi"
    val after = "abcde<tag name = \"name\"></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInClose() {
    val keys = listOf("dit")
    val before = "abcde<tag>fg</ta${c}g>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockAfter() {
    val keys = listOf("dit")
    val before = "abcde<tag>fg</tag>h${c}i"
    val after = "abcde<tag>fg</tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInAlone() {
    val keys = listOf("dit")
    val before = "abcde<ta${c}g>fghi"
    val after = "abcde<tag>fghi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockWithoutTags() {
    val keys = listOf("dit")
    val before = "abc${c}de"
    val after = "abcde"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBeforeWithoutOpenTag() {
    val keys = listOf("dit")
    val before = "abc${c}defg</tag>hi"
    val after = "abcdefg</tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInCloseWithoutOpenTag() {
    val keys = listOf("dit")
    val before = "abcdefg</ta${c}g>hi"
    val after = "abcdefg</tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockAfterWithoutOpenTag() {
    val keys = listOf("dit")
    val before = "abcdefg</tag>h${c}i"
    val after = "abcdefg</tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBeforeWithoutCloseTag() {
    val keys = listOf("dit")
    val before = "abc${c}defg<tag>hi"
    val after = "abcdefg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInOpenWithoutCloseTag() {
    val keys = listOf("dit")
    val before = "abcdefg<ta${c}g>hi"
    val after = "abcdefg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockAfterWithoutCloseTag() {
    val keys = listOf("dit")
    val before = "abcdefg<tag>h${c}i"
    val after = "abcdefg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBeforeWrongOrder() {
    val keys = listOf("dit")
    val before = "abc${c}de</tag>fg<tag>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInOpenWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</ta${c}g>fg<tag>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBetweenWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</tag>f${c}g<tag>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockInCloseWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</tag>fg<ta${c}g>hi"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testDeleteInnerTagBlockTwoTagsWrongOrder() {
    val keys = listOf("dit")
    val before = "<foo><html>t${c}ext</foo></html>"
    val after = "<foo></foo></html>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testDeleteInnerTagBlockTwoTagsWrongOrderInClosingTag() {
    val keys = listOf("dit")
    val before = "<foo><html>text</foo></htm${c}l>"
    val after = "<foo><html></html>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockAfterWrongOrder() {
    val keys = listOf("dit")
    val before = "abcde</tag>fg<tag>h${c}i"
    val after = "abcde</tag>fg<tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBracketInside() {
    val keys = listOf("dit")
    val before = "abcde<tag>f$c<>g</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagBlockBracketInsideString() {
    val keys = listOf("dit")
    val before = "abcde<tag>f${c}\"<>\"g</tag>hi"
    val after = "abcde<tag></tag>hi"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagIsCaseInsensitive() {
    val keys = listOf("dit")
    val before = "<a> <as${c}df> </A>"
    val after = "<a></A>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |d| |v_it|
  @Test
  fun testDeleteInnerTagSlashesInAttribute() {
    val keys = listOf("dit")
    val before = "<a href=\"https://isitchristmas.com\" class=\"button\">Bing ${c}Bing bing</a>"
    val after = "<a href=\"https://isitchristmas.com\" class=\"button\"></a>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // VIM-1090 |d| |v_it|
  // Adapted from vim source file "test_textobjects.vim"
  @Test
  fun testDeleteInnerTagDuplicateTags() {
    val keys = listOf("dit")
    val before = "<b>as${c}d<i>as<b />df</i>asdf</b>"
    val after = "<b></b>"
    doTest(keys, before, after, Mode.NORMAL())
  }

  // |v_it|
  @Test
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
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertPluginError(true)
  }

  // |v_it|
  @Test
  fun testSelectInnerTagEmptyTag() {
    val before = "<a>$c</a>"
    val after = "$s<a></a$c>$se"
    configureByText(before)
    val keys = listOf("vit")
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test single character`() {
    // The whole tag block is also selected if there is only a single character inside
    val before = "<a>${c}a</a>"
    val after = "$s<a>a</a$c>$se"
    val keys = listOf("vit")
    doTest(keys, before, after, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test single character inside tag`() {
    configureByText("<a$c></a>")
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("<")
  }

  // VIM-1633 |v_it|
  @Test
  fun testNestedInTagSelection() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("Inner")
  }

  @Test
  fun `test nested tag double motion`() {
    configureByText(
      "<o>Outer\n" +
        " $c  <t></t>\n" +
        "</o>\n",
    )
    typeText(injector.parser.parseKeys("vitit"))
    assertSelection("<t></t>")
  }

  @Test
  fun `test in inner tag double motion`() {
    configureByText("<o><t>$c</t>\n</o>")
    typeText(injector.parser.parseKeys("vitit"))
    assertSelection("<o><t></t>\n</o>")
  }

  @Test
  fun `test nested tags between tags`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t> $c <t>Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>  <t>Inner</t>")
  }

  @Test
  fun `test nested tags number motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("v2it"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  @Test
  fun `test nested tags double motion`() {
    configureByText(
      "<o>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</o>\n",
    )
    typeText(injector.parser.parseKeys("vitit"))
    assertSelection("<t>Inner</t>")
  }

  @Test
  fun `test nested tags triple motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vititit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  @Test
  fun `test nested tags in closing tag`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</${c}t>\n",
    )
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  @Test
  fun `test nested tags in opening tag`() {
    configureByText(
      "<${c}t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  @Test
  fun `test nested tags ouside tag`() {
    configureByText(
      "$c<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("Outer\n" + "   <t>Inner</t>")
  }

  @Test
  fun `test skip whitespace at start of line`() {
    configureByText(
      "<o>Outer\n" +
        " $c  <t></t>\n" +
        "</o>\n",
    )
    typeText(injector.parser.parseKeys("vit"))
    assertSelection("<")
  }

  // ============== preserveSelectionAnchor behavior tests ==============

  @Test
  fun `test inner tag from middle of content`() {
    doTest(
      "vit",
      "<div>foo b${c}ar baz</div>",
      "<div>${s}foo bar ba${c}z${se}</div>",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test inner tag with backwards selection`() {
    doTest(
      listOf("v", "h", "it"),
      "<div>foo b${c}ar baz</div>",
      "<div>${s}${c}foo bar baz${se}</div>",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
