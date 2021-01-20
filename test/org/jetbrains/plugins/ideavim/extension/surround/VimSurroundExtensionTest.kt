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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.extension.surround

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author dhleong
 */
class VimSurroundExtensionTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("surround")
  }

  /* surround */

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest("yseb", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("yse)", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("yse(", before, "if ( condition ) {\n" + "}\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testSurroundWORDBlock() {
    val before = "if (condition) ${c}return;\n"
    val after = "if (condition) {return;}\n"

    doTest("ysEB", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ysE}", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ysE{", before, "if (condition) { return; }\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testSurroundWordArray() {
    val before = "int foo = bar${c}index;"
    val after = "int foo = bar[index];"

    doTest("yser", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("yse]", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("yse[", before, "int foo = bar[ index ];", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testSurroundWordAngle() {
    val before = "foo = new Bar${c}Baz();"
    val after = "foo = new Bar<Baz>();"

    doTest("ysea", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("yse>", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testSurroundQuotes() {
    val before = "foo = ${c}new Bar.Baz;"
    val after = "foo = \"new Bar.Baz\";"

    doTest("yst;\"", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ys4w\"", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testSurroundTag() {
    configureByText("Hello ${c}World!\n")
    typeText(parseKeys("ysiw\\<em>"))
    myFixture.checkResult("Hello <em>World</em>!\n")
  }

  // VIM-1569
  fun testSurroundTagWithAttributes() {
    configureByText("Hello ${c}World!")
    typeText(parseKeys("ysiw\\<span class=\"important\" data-foo=\"bar\">"))
    myFixture.checkResult("Hello <span class=\"important\" data-foo=\"bar\">World</span>!")
  }

  // VIM-1569
  fun testSurraungTagAsInIssue() {
    configureByText("<p>${c}Hello</p>")
    typeText(parseKeys("VS<div class = \"container\">"))
    myFixture.checkResult("<div class = \"container\"><p>Hello</p></div>")
  }

  fun testSurroundFunctionName() {
    configureByText("foo = b${c}ar")
    typeText(parseKeys("ysiwfbaz"))
    myFixture.checkResult("foo = ${c}baz(bar)")
  }

  fun testSurroundFunctionNameDoesNothingIfInputIsEmpty() {
    // The cursor does not move. This is different from Vim
    // where the cursor moves to the beginning of the text object.
    configureByText("foo = b${c}ar")
    typeText(parseKeys("ysiwf"))
    myFixture.checkResult("foo = b${c}ar")
  }

  fun testSurroundFunctionNameWithInnerSpacing() {
    configureByText("foo = b${c}ar")
    typeText(parseKeys("ysiwFbaz"))
    myFixture.checkResult("foo = ${c}baz( bar )")
  }

  fun testSurroundSpace() {
    configureByText("foo(b${c}ar)")
    typeText(parseKeys("csbs"))
    myFixture.checkResult("foo${c} bar")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatSurround() {
    val before = "if ${c}condition {\n}\n"
    val after = "if ((condition)) {\n}\n"

    doTest(listOf("ysiw)", "l", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatSurroundDouble() {
    val before = "if ${c}condition {\n}\n"
    val after = "if (((condition))) {\n}\n"

    doTest(listOf("ysiw)", "l", ".", "l", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatDifferentChanges() {
    val before = """
                  if "${c}condition" { }
                  if "condition" { }
                    """
    val after = """
                  if '(condition)' { }
                  if 'condition' { }
                    """

    doTest(listOf("ysiw)", "cs\"'", "j", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatWrapWithFunction() {
    val before = """
                  if "${c}condition" { }
                  if "condition" { }
                    """
    val after = """
                  if "myFunction(condition)" { }
                  if "myFunction(condition)" { }
                    """

    doTest(listOf("ysiwf", "myFunction<CR>", "j", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatWrapWithTag() {
    val before = """
                  ${c}abc
                  abc
                    """
    val after = """
                  <myTag>abc</myTag>
                  <myTag>abc</myTag>
                    """

    doTest(listOf("ysiwt", "myTag>", "j", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  /* visual surround */

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testVisualSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest("veSb", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
    doTest("veS)", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
    doTest("veS(", before,
      "if ( condition ) {\n" + "}\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  /* Delete surroundings */

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if condition {\n" + "}\n"

    doTest("dsb", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds(", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds)", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingQuote() {
    val before = "if (\"${c}foo\".equals(foo)) {\n" + "}\n"
    val after = "if (${c}foo.equals(foo)) {\n" + "}\n"

    doTest("ds\"", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingBlock() {
    val before = "if (condition) {${c}return;}\n"
    val after = "if (condition) return;\n"

    doTest(listOf("dsB"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds}", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds{", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingArray() {
    val before = "int foo = bar[${c}index];"
    val after = "int foo = barindex;"

    doTest("dsr", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds]", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds[", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingAngle() {
    val before = "foo = new Bar<${c}Baz>();"
    val after = "foo = new BarBaz();"

    doTest("dsa", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds>", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest("ds<", before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}Foo</div>"

    doTest(listOf("dst"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1085
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteSurroundingParamsAtLineEnd() {
    val before = "Foo\n" + "Seq(\"-${c}Yrangepos\")\n"
    val after = "Foo\n" + "Seq\"-Yrangepos\"\n"

    doTest(listOf("dsb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1085
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testDeleteMultiLineSurroundingParamsAtLineEnd() {
    val before = "Foo\n" +
      "Bar\n" +
      "Seq(\"-${c}Yrangepos\",\n" +
      "    other)\n" +
      "Baz\n"
    val after = "Foo\n" +
      "Bar\n" +
      "Seq\"-Yrangepos\",\n" +
      "    other\n" +
      "Baz\n"

    doTest(listOf("dsb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatDeleteSurroundParens() {
    val before = "if ((${c}condition)) {\n}\n"
    val after = "if condition {\n}\n"

    doTest(listOf("dsb."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatDeleteSurroundQuotes() {
    val before = "if (\"${c}condition\") {\n}\n"
    val after = "if (condition) {\n}\n"

    doTest(listOf("ds\"."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  /* Change surroundings */

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testChangeSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if [condition] {\n" + "}\n"

    doTest(listOf("csbr"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testChangeSurroundingBlock() {
    val before = "if (condition) {${c}return;}"
    val after = "if (condition) (return;)"

    doTest(listOf("csBb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testChangeSurroundingTagSimple() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}(Foo)</div>"

    doTest(listOf("cstb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testChangeSurroundingTagAnotherTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}<b>Foo</b></div>"

    doTest(listOf("cst\\<b>"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testRepeatChangeSurroundingParens() {
    val before = "foo(${c}index)(index2) = bar;"
    val after = "foo[index][index2] = bar;"

    doTest(listOf("csbrE."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
      <h1>Title</h1>
      
      <p>
      SurroundThis
      </p>
      
      <p>Some text</p>
  """)
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test wrap with tag full line`() {
    doTest(listOf("VS\\<p>"), """
      <h1>Title</h1>
      
      Sur${c}roundThis
      
      <p>Some text</p>
    """.trimIndent(), """
      <h1>Title</h1>
      
      <p>SurroundThis
      </p>
      <p>Some text</p>
    """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
      <div>
          <p>Some paragraph</p>
          <p>
          Surround This
          </p>
          <p>Some other paragraph</p>
      </div>
  """)
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test wrap with tag full line in middle`() {
    doTest(listOf("VS\\<p>"), """
      <div>
          <p>Some paragraph</p>
          Sur${c}round This
          <p>Some other paragraph</p>
      </div>
      """.trimIndent(), """
      <div>
          <p>Some paragraph</p>
      <p>    Surround This
      </p>    <p>Some other paragraph</p>
      </div>
    """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun `test wrap line with char selection`() {
    doTest(listOf("vawES\\<p>"), """
      <div>
          <p>Some paragraph</p>
          Sur${c}round This
          <p>Some other paragraph</p>
      </div>
      """.trimIndent(), """
      <div>
          <p>Some paragraph</p>
          <p>Surround This</p>
          <p>Some other paragraph</p>
      </div>
    """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  fun testWithAnExistingMapping() {
    val before = "(foo)"
    val after = "[foo]"

    configureByText(before)

    typeText(commandToKeys("noremap d <C-d>"))
    typeText(parseKeys("cs(]"))
    myFixture.checkResult(after)
  }
}
