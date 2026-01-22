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
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionOuterBlockTagActionTest : VimTestCase() {

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBefore() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}de<tag>fg</tag>hi")
    assertState("abcde<tag>fg</tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInOpen() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<ta${c}g>fg</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInOpenWithArgs() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<ta${c}g name = \"name\">fg</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBetween() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag>f${c}g</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBetweenWithArgs() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag name = \"name\">f${c}g</tag>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInClose() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag>fg</ta${c}g>hi")
    assertState("abcdehi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockAfter() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<tag>fg</tag>h${c}i")
    assertState("abcde<tag>fg</tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInAlone() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde<ta${c}g>fghi")
    assertState("abcde<tag>fghi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockWithoutTags() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}de")
    assertState("abcde")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBeforeWithoutOpenTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}defg</tag>hi")
    assertState("abcdefg</tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInCloseWithoutOpenTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg</ta${c}g>hi")
    assertState("abcdefg</tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockAfterWithoutOpenTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg</tag>h${c}i")
    assertState("abcdefg</tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBeforeWithoutCloseTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}defg<tag>hi")
    assertState("abcdefg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInOpenWithoutCloseTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg<ta${c}g>hi")
    assertState("abcdefg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockAfterWithoutCloseTag() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcdefg<tag>h${c}i")
    assertState("abcdefg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBeforeWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abc${c}de</tag>fg<tag>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInOpenWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</ta${c}g>fg<tag>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockBetweenWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</tag>f${c}g<tag>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockInCloseWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</tag>fg<ta${c}g>hi")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_at|
  @Test
  fun testDeleteOuterTagBlockAfterWrongOrder() {
    typeTextInFile(injector.parser.parseKeys("dat"), "abcde</tag>fg<tag>h${c}i")
    assertState("abcde</tag>fg<tag>hi")
  }

  // |d| |v_it|
  @Test
  @VimBehaviorDiffers(shouldBeFixed = true)
  fun testDeleteInnerTagAngleBrackets() {
    doTest(
      "dit",
      "<div ${c}hello=\"d > hsj < akl\"></div>",
      "<div hello=\"d ></div>",
    )
  }

  // VIM-1090 |d| |v_at|
  @Test
  fun testDeleteOuterTagDuplicateTags() {
    typeTextInFile(injector.parser.parseKeys("dat"), "<a><a></a></a$c>")
    assertState("")
  }

  // |v_it| |v_at|
  @Test
  fun testTagSelectionSkipsWhitespaceAtStartOfLine() {
    // Also skip tabs
    configureByText(
      "<o>Outer\n" +
        " $c \t <t>Inner</t>\n" +
        "</o>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection("<t>Inner</t>")
  }

  @Test
  fun `test skip new line`() {
    // Newline must not be skipped
    configureByText("${c}\n" + "    <t>asdf</t>")
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(null)
  }

  @Test
  fun `test whitespace skip`() {
    // Whitespace is only skipped if there is nothing else at the start of the line
    configureByText(
      "<o>Outer\n" +
        "a $c  <t>Inner</t>\n" +
        "</o>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<o>Outer\n" +
        "a   <t>Inner</t>\n" +
        "</o>",
    )
  }

  // |v_at|
  @Test
  fun testNestedTagSelection() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection("<t>Inner</t>")
  }

  @Test
  fun `test nested tags between tags`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t> $c <t>Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>  <t>Inner</t>\n" +
        "</t>",
    )
  }

  @Test
  fun `test nested tags double motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vatat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>",
    )
  }

  @Test
  fun `test nested tags number motion`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>${c}Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("v2at"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>",
    )
  }

  @Test
  fun `test nested tags on outer`() {
    configureByText(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</${c}t>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>",
    )
  }

  @Test
  fun `test nested tags on outer start`() {
    configureByText(
      "<${c}t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>",
    )
  }

  @Test
  fun `test nested tags outside outer`() {
    configureByText(
      "$c<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>\n",
    )
    typeText(injector.parser.parseKeys("vat"))
    assertSelection(
      "<t>Outer\n" +
        "   <t>Inner</t>\n" +
        "</t>",
    )
  }

  // ============== preserveSelectionAnchor behavior tests ==============

  @Test
  fun `test outer tag from middle of content`() {
    doTest(
      "vat",
      "<div>foo b${c}ar baz</div>",
      "${s}<div>foo bar baz</div${c}>${se}",
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
  fun `test outer tag with backwards selection`() {
    doTest(
      listOf("v", "h", "at"),
      "<div>foo b${c}ar baz</div>",
      "${s}${c}<div>foo bar baz</div>${se}",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
