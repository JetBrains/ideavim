/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

  fun testSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest(parseKeys("yseb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("yse)"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("yse("), before,
      "if ( condition ) {\n" + "}\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testSurroundWORDBlock() {
    val before = "if (condition) ${c}return;\n"
    val after = "if (condition) {return;}\n"

    doTest(parseKeys("ysEB"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ysE}"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ysE{"), before, "if (condition) { return; }\n", CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
  }

  fun testSurroundWordArray() {
    val before = "int foo = bar${c}index;"
    val after = "int foo = bar[index];"

    doTest(parseKeys("yser"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("yse]"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("yse["), before, "int foo = bar[ index ];", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testSurroundWordAngle() {
    val before = "foo = new Bar${c}Baz();"
    val after = "foo = new Bar<Baz>();"

    doTest(parseKeys("ysea"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("yse>"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testSurroundQuotes() {
    val before = "foo = ${c}new Bar.Baz;"
    val after = "foo = \"new Bar.Baz\";"

    doTest(parseKeys("yst;\""), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ys4w\""), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
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

  fun testRepeatSurround() {
    val before = "if ${c}condition {\n}\n"
    val after = "if ((condition)) {\n}\n"

    doTest(parseKeys("ysiw)", "l", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatSurroundDouble() {
    val before = "if ${c}condition {\n}\n"
    val after = "if (((condition))) {\n}\n"

    doTest(parseKeys("ysiw)", "l", ".", "l", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatDifferentChanges() {
    val before = """
                  if "${c}condition" { }
                  if "condition" { }
                    """
    val after = """
                  if '(condition)' { }
                  if 'condition' { }
                    """

    doTest(parseKeys("ysiw)", "cs\"'", "j", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatWrapWithFunction() {
    val before = """
                  if "${c}condition" { }
                  if "condition" { }
                    """
    val after = """
                  if "myFunction(condition)" { }
                  if "myFunction(condition)" { }
                    """

    doTest(parseKeys("ysiwf", "myFunction<CR>", "j", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatWrapWithTag() {
    val before = """
                  ${c}abc
                  abc
                    """
    val after = """
                  <myTag>abc</myTag>
                  <myTag>abc</myTag>
                    """

    doTest(parseKeys("ysiwt", "myTag>", "j", "."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  /* visual surround */

  fun testVisualSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest(parseKeys("veSb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
    doTest(parseKeys("veS)"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
    doTest(parseKeys("veS("), before,
      "if ( condition ) {\n" + "}\n", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }

  /* Delete surroundings */

  fun testDeleteSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if condition {\n" + "}\n"

    doTest(parseKeys("dsb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds("), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds)"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteSurroundingQuote() {
    val before = "if (\"${c}foo\".equals(foo)) {\n" + "}\n"
    val after = "if (${c}foo.equals(foo)) {\n" + "}\n"

    doTest(parseKeys("ds\""), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteSurroundingBlock() {
    val before = "if (condition) {${c}return;}\n"
    val after = "if (condition) return;\n"

    doTest(parseKeys("dsB"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds}"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds{"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteSurroundingArray() {
    val before = "int foo = bar[${c}index];"
    val after = "int foo = barindex;"

    doTest(parseKeys("dsr"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds]"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds["), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteSurroundingAngle() {
    val before = "foo = new Bar<${c}Baz>();"
    val after = "foo = new BarBaz();"

    doTest(parseKeys("dsa"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds>"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    doTest(parseKeys("ds<"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testDeleteSurroundingTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}Foo</div>"

    doTest(parseKeys("dst"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1085
  fun testDeleteSurroundingParamsAtLineEnd() {
    val before = "Foo\n" + "Seq(\"-${c}Yrangepos\")\n"
    val after = "Foo\n" + "Seq\"-Yrangepos\"\n"

    doTest(parseKeys("dsb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1085
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

    doTest(parseKeys("dsb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatDeleteSurroundParens() {
    val before = "if ((${c}condition)) {\n}\n"
    val after = "if condition {\n}\n"

    doTest(parseKeys("dsb."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatDeleteSurroundQuotes() {
    val before = "if (\"${c}condition\") {\n}\n"
    val after = "if (condition) {\n}\n"

    doTest(parseKeys("ds\"."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  /* Change surroundings */

  fun testChangeSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if [condition] {\n" + "}\n"

    doTest(parseKeys("csbr"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testChangeSurroundingBlock() {
    val before = "if (condition) {${c}return;}"
    val after = "if (condition) (return;)"

    doTest(parseKeys("csBb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testChangeSurroundingTagSimple() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}(Foo)</div>"

    doTest(parseKeys("cstb"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testChangeSurroundingTagAnotherTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}<b>Foo</b></div>"

    doTest(parseKeys("cst\\<b>"), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun testRepeatChangeSurroundingParens() {
    val before = "foo(${c}index)(index2) = bar;"
    val after = "foo[index][index2] = bar;"

    doTest(parseKeys("csbrE."), before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
      <h1>Title</h1>
      
      <p>
      SurroundThis
      </p>
      
      <p>Some text</p>
  """)
  fun `test wrap with tag full line`() {
    doTest(parseKeys("VS\\<p>"), """
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
  fun `test wrap with tag full line in middle`() {
    doTest(parseKeys("VS\\<p>"), """
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

  fun `test wrap line with char selection`() {
    doTest(parseKeys("vawES\\<p>"), """
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
}
