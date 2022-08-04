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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.extension.surround

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
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

    doTest("yseb", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yse)", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yse(", before, "if ( condition ) {\n" + "}\n", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundWORDBlock() {
    val before = "if (condition) ${c}return;\n"
    val after = "if (condition) {return;}\n"

    doTest("ysEB", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ysE}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ysE{", before, "if (condition) { return; }\n", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundWordArray() {
    val before = "int foo = bar${c}index;"
    val after = "int foo = bar[index];"

    doTest("yser", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yse]", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yse[", before, "int foo = bar[ index ];", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundWordAngle() {
    val before = "foo = new Bar${c}Baz();"
    val after = "foo = new Bar<Baz>();"

    doTest("ysea", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yse>", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundQuotes() {
    val before = "foo = ${c}new Bar.Baz;"
    val after = "foo = \"new Bar.Baz\";"

    doTest("yst;\"", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ys4w\"", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundTag() {
    configureByText("Hello ${c}World!\n")
    typeText(injector.parser.parseKeys("ysiw<em>"))
    assertState("Hello <em>World</em>!\n")
  }

  // VIM-1569
  fun testSurroundTagWithAttributes() {
    configureByText("Hello ${c}World!")
    typeText(injector.parser.parseKeys("ysiw<span class=\"important\" data-foo=\"bar\">"))
    assertState("Hello <span class=\"important\" data-foo=\"bar\">World</span>!")
  }

  // VIM-1569
  fun testSurraungTagAsInIssue() {
    configureByText("<p>${c}Hello</p>")
    typeText(injector.parser.parseKeys("VS<div class = \"container\">"))
    assertState("<div class = \"container\"><p>Hello</p></div>")
  }

  fun testSurroundCustomElement() {
    configureByText("${c}Click me!")
    typeText(injector.parser.parseKeys("VS<custom-button>"))
    assertState("<custom-button>Click me!</custom-button>")
  }

  fun testSurroundFunctionName() {
    configureByText("foo = b${c}ar")
    typeText(injector.parser.parseKeys("ysiwfbaz"))
    assertState("foo = ${c}baz(bar)")
  }

  fun testSurroundFunctionNameDoesNothingIfInputIsEmpty() {
    // The cursor does not move. This is different from Vim
    // where the cursor moves to the beginning of the text object.
    configureByText("foo = b${c}ar")
    typeText(injector.parser.parseKeys("ysiwf"))
    assertState("foo = b${c}ar")
  }

  fun testSurroundFunctionNameWithInnerSpacing() {
    configureByText("foo = b${c}ar")
    typeText(injector.parser.parseKeys("ysiwFbaz"))
    assertState("foo = ${c}baz( bar )")
  }

  fun testSurroundSpace() {
    configureByText("foo(b${c}ar)")
    typeText(injector.parser.parseKeys("csbs"))
    assertState("foo${c} bar")
  }

  fun testRepeatSurround() {
    val before = "if ${c}condition {\n}\n"
    val after = "if ((condition)) {\n}\n"

    doTest(listOf("ysiw)", "l", "."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testRepeatSurroundDouble() {
    val before = "if ${c}condition {\n}\n"
    val after = "if (((condition))) {\n}\n"

    doTest(listOf("ysiw)", "l", ".", "l", "."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
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

    doTest(listOf("ysiw)", "cs\"'", "j", "."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
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

    doTest(
      listOf("ysiwf", "myFunction<CR>", "j", "."),
      before,
      after,
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
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

    doTest(listOf("ysiwt", "myTag>", "j", "."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  /* visual surround */

  fun testVisualSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest("veSb", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertMode(VimStateMachine.Mode.COMMAND)
    doTest("veS)", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertMode(VimStateMachine.Mode.COMMAND)
    doTest(
      "veS(", before,
      "if ( condition ) {\n" + "}\n", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
    assertMode(VimStateMachine.Mode.COMMAND)
  }

  /* Delete surroundings */

  fun testDeleteSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if condition {\n" + "}\n"

    doTest("dsb", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds(", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds)", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDeleteSurroundingQuote() {
    val before = "if (\"${c}foo\".equals(foo)) {\n" + "}\n"
    val after = "if (${c}foo.equals(foo)) {\n" + "}\n"

    doTest("ds\"", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDeleteSurroundingBlock() {
    val before = "if (condition) {${c}return;}\n"
    val after = "if (condition) return;\n"

    doTest(listOf("dsB"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDeleteSurroundingArray() {
    val before = "int foo = bar[${c}index];"
    val after = "int foo = barindex;"

    doTest("dsr", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds]", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds[", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDeleteSurroundingAngle() {
    val before = "foo = new Bar<${c}Baz>();"
    val after = "foo = new BarBaz();"

    doTest("dsa", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds>", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("ds<", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDeleteSurroundingTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}Foo</div>"

    doTest(listOf("dst"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  // VIM-1085
  fun testDeleteSurroundingParamsAtLineEnd() {
    val before = "Foo\n" + "Seq(\"-${c}Yrangepos\")\n"
    val after = "Foo\n" + "Seq\"-Yrangepos\"\n"

    doTest(listOf("dsb"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
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

    doTest(listOf("dsb"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  // VIM-2227
  fun testDeleteInvalidSurroundingCharacter() {
    val text = "if (${c}condition) {"

    doTest("yibds]", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yibds[", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yibds}", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yibds{", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testRepeatDeleteSurroundParens() {
    val before = "if ((${c}condition)) {\n}\n"
    val after = "if condition {\n}\n"

    doTest(listOf("dsb."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testRepeatDeleteSurroundQuotes() {
    val before = "if (\"${c}condition\") {\n}\n"
    val after = "if (condition) {\n}\n"

    doTest(listOf("ds\"."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  /* Change surroundings */

  fun testChangeSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if [condition] {\n" + "}\n"

    doTest(listOf("csbr"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testChangeSurroundingBlock() {
    val before = "if (condition) {${c}return;}"
    val after = "if (condition) (return;)"

    doTest(listOf("csBb"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testChangeSurroundingTagSimple() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}(Foo)</div>"

    doTest(listOf("cstb"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testChangeSurroundingTagAnotherTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}<b>Foo</b></div>"

    doTest(listOf("cst<b>"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testRepeatChangeSurroundingParens() {
    val before = "foo(${c}index)(index2) = bar;"
    val after = "foo[index][index2] = bar;"

    doTest(listOf("csbrE."), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  // VIM-2227
  fun testChangeInvalidSurroundingCharacter() {
    val text = "if (${c}condition) {"

    doTest("yibcs]}", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yibcs[}", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yibcs}]", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    doTest("yibcs{]", text, text, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @VimBehaviorDiffers(
    """
      <h1>Title</h1>
      
      <p>
      SurroundThis
      </p>
      
      <p>Some text</p>
  """
  )
  fun `test wrap with tag full line`() {
    doTest(
      listOf("VS<p>"),
      """
      <h1>Title</h1>
      
      Sur${c}roundThis
      
      <p>Some text</p>
      """.trimIndent(),
      """
      <h1>Title</h1>
      
      <p>SurroundThis
      </p>
      <p>Some text</p>
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  @VimBehaviorDiffers(
    """
      <div>
          <p>Some paragraph</p>
          <p>
          Surround This
          </p>
          <p>Some other paragraph</p>
      </div>
  """
  )
  fun `test wrap with tag full line in middle`() {
    doTest(
      listOf("VS<p>"),
      """
      <div>
          <p>Some paragraph</p>
          Sur${c}round This
          <p>Some other paragraph</p>
      </div>
      """.trimIndent(),
      """
      <div>
          <p>Some paragraph</p>
      <p>    Surround This
      </p>    <p>Some other paragraph</p>
      </div>
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test wrap line with char selection`() {
    doTest(
      listOf("vawES<p>"),
      """
      <div>
          <p>Some paragraph</p>
          Sur${c}round This
          <p>Some other paragraph</p>
      </div>
      """.trimIndent(),
      """
      <div>
          <p>Some paragraph</p>
          <p>Surround This</p>
          <p>Some other paragraph</p>
      </div>
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun testWithAnExistingMapping() {
    val before = "(foo)"
    val after = "[foo]"

    configureByText(before)

    typeText(commandToKeys("noremap d <C-d>"))
    typeText(injector.parser.parseKeys("cs(]"))
    assertState(after)
  }

  fun `test change new line`() {
    val before = """
      "\n"
    """.trimIndent()
    configureByText(before)

    typeText(injector.parser.parseKeys("cs\"'"))
    val after = """'\n'"""
    assertState(after)
  }

  fun testMappingSurroundPlugin() {
    val before = "if (condition) ${c}return;\n"
    val after = "if (condition) \"return\";\n"

    doTest(":map gw ysiw\"<CR>gw", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundPluginWithMacro() {
    val before = """
      if (con${c}dition) return;
      if (condition) return;
    """.trimIndent()
    val after = """
      if condition return;
      if ${c}condition return;
    """.trimIndent()

    doTest("qqds)qj@q", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testSurroundPluginWithMacroAndMapping() {
    val before = """
      if (con${c}dition) return;
      if (condition) return;
    """.trimIndent()
    val after = """
      if condition return;
      if ${c}condition return;
    """.trimIndent()

    val keys = ":map gw ds)<CR>" + "qqgwqj@q"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test change surround with multicaret`() {
    val before = """
                  (${c}abc)
                  (${c}xyz)
                    """
    val after = """
                  [abc]
                  [xyz]
                    """

    doTest(listOf("cs(]"), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test delete surround with multicaret`() {
    val before = """
                  (${c}abc)
                  (${c}xyz)
                    """
    val after = """
                  abc
                  xyz
                    """

    doTest(listOf("ds("), before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }
}
