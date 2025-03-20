/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.extension.surround

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.sneak.IdeaVimSneakExtension
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * @author dhleong
 */
class VimSurroundExtensionTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("surround")
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    IdeaVimSneakExtension.stopTimer()
    super.tearDown(testInfo)
  }

  /* surround */

  @Test
  fun testSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest("yseb", before, after, Mode.NORMAL())
    doTest("yse)", before, after, Mode.NORMAL())
    doTest("yse(", before, "if ( condition ) {\n" + "}\n", Mode.NORMAL())
  }

  @Test
  fun testSurroundWORDBlock() {
    val before = "if (condition) ${c}return;\n"
    val after = "if (condition) {return;}\n"

    doTest("ysEB", before, after, Mode.NORMAL())
    doTest("ysE}", before, after, Mode.NORMAL())
    doTest("ysE{", before, "if (condition) { return; }\n", Mode.NORMAL())
  }

  @Test
  fun testSurroundWordArray() {
    val before = "int foo = bar${c}index;"
    val after = "int foo = bar[index];"

    doTest("yser", before, after, Mode.NORMAL())
    doTest("yse]", before, after, Mode.NORMAL())
    doTest("yse[", before, "int foo = bar[ index ];", Mode.NORMAL())
  }

  @Test
  fun testSurroundWordAngle() {
    val before = "foo = new Bar${c}Baz();"
    val after = "foo = new Bar<Baz>();"

    doTest("ysea", before, after, Mode.NORMAL())
    doTest("yse>", before, after, Mode.NORMAL())
  }

  @Test
  fun testSurroundQuotes() {
    val before = "foo = ${c}new Bar.Baz;"
    val after = "foo = \"new Bar.Baz\";"

    doTest("yst;\"", before, after, Mode.NORMAL())
    doTest("ys4w\"", before, after, Mode.NORMAL())
  }

  @Test
  fun testSurroundTag() {
    configureByText("Hello ${c}World!\n")
    typeText(injector.parser.parseKeys("ysiw<em>"))
    assertState("Hello <em>World</em>!\n")
  }

  // VIM-1569
  @Test
  fun testSurroundTagWithAttributes() {
    configureByText("Hello ${c}World!")
    typeText(injector.parser.parseKeys("ysiw<span class=\"important\" data-foo=\"bar\">"))
    assertState("Hello <span class=\"important\" data-foo=\"bar\">World</span>!")
  }

  // VIM-1569
  @Test
  fun testSurraungTagAsInIssue() {
    configureByText("<p>${c}Hello</p>")
    typeText(injector.parser.parseKeys("VS<div class = \"container\">"))
    assertState("<div class = \"container\">\n<p>Hello</p>\n</div>")
  }

  @Test
  fun testSurroundCustomElement() {
    configureByText("${c}Click me!")
    typeText(injector.parser.parseKeys("VS<custom-button>"))
    assertState("<custom-button>\nClick me!\n</custom-button>")
  }

  @Test
  fun testSurroundFunctionName() {
    configureByText("foo = b${c}ar")
    typeText(injector.parser.parseKeys("ysiwfbaz"))
    assertState("foo = ${c}baz(bar)")
  }

  @Test
  fun testSurroundFunctionNameDoesNothingIfInputIsEmpty() {
    // The cursor does not move. This is different from Vim
    // where the cursor moves to the beginning of the text object.
    configureByText("foo = b${c}ar")
    typeText(injector.parser.parseKeys("ysiwf"))
    assertState("foo = b${c}ar")
  }

  @Test
  fun testSurroundFunctionNameWithInnerSpacing() {
    configureByText("foo = b${c}ar")
    typeText(injector.parser.parseKeys("ysiwFbaz"))
    assertState("foo = ${c}baz( bar )")
  }

  @Test
  fun testSurroundSpace() {
    configureByText("foo(b${c}ar)")
    typeText(injector.parser.parseKeys("csbs"))
    assertState("foo${c} bar")
  }

  @Test
  fun testRepeatSurround() {
    val before = "if ${c}condition {\n}\n"
    val after = "if ((condition)) {\n}\n"

    doTest(listOf("ysiw)", "l", "."), before, after, Mode.NORMAL())
  }

  @Test
  fun testRepeatSurroundDouble() {
    val before = "if ${c}condition {\n}\n"
    val after = "if (((condition))) {\n}\n"

    doTest(listOf("ysiw)", "l", ".", "l", "."), before, after, Mode.NORMAL())
  }

  @Test
  fun testRepeatDifferentChanges() {
    val before = """
                  if "${c}condition" { }
                  if "condition" { }
                    """
    val after = """
                  if '(condition)' { }
                  if 'condition' { }
                    """

    doTest(listOf("ysiw)", "cs\"'", "j", "."), before, after, Mode.NORMAL())
  }

  @Test
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
      Mode.NORMAL(),
    )
  }

  @Test
  fun testRepeatWrapWithTag() {
    val before = """
                  ${c}abc
                  abc
                    """
    val after = """
                  <myTag>abc</myTag>
                  <myTag>abc</myTag>
                    """

    doTest(listOf("ysiwt", "myTag>", "j", "."), before, after, Mode.NORMAL())
  }

  /* visual surround */

  @Test
  fun testVisualSurroundWordParens() {
    val before = "if ${c}condition {\n" + "}\n"
    val after = "if ${c}(condition) {\n" + "}\n"

    doTest("veSb", before, after, Mode.NORMAL())
    assertMode(Mode.NORMAL())
    doTest("veS)", before, after, Mode.NORMAL())
    assertMode(Mode.NORMAL())
    doTest(
      "veS(",
      before,
      "if ( condition ) {\n" + "}\n",
      Mode.NORMAL(),
    )
    assertMode(Mode.NORMAL())
  }

  /* Delete surroundings */

  @Test
  fun testDeleteSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if condition {\n" + "}\n"

    doTest("dsb", before, after, Mode.NORMAL())
    doTest("ds(", before, after, Mode.NORMAL())
    doTest("ds)", before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteSurroundingQuote() {
    val before = "if (\"${c}foo\".equals(foo)) {\n" + "}\n"
    val after = "if (${c}foo.equals(foo)) {\n" + "}\n"

    doTest("ds\"", before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteSurroundingBlock() {
    val before = "if (condition) {${c}return;}\n"
    val after = "if (condition) return;\n"

    doTest(listOf("dsB"), before, after, Mode.NORMAL())
    doTest("ds}", before, after, Mode.NORMAL())
    doTest("ds{", before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteSurroundingArray() {
    val before = "int foo = bar[${c}index];"
    val after = "int foo = barindex;"

    doTest("dsr", before, after, Mode.NORMAL())
    doTest("ds]", before, after, Mode.NORMAL())
    doTest("ds[", before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteSurroundingAngle() {
    val before = "foo = new Bar<${c}Baz>();"
    val after = "foo = new BarBaz();"

    doTest("dsa", before, after, Mode.NORMAL())
    doTest("ds>", before, after, Mode.NORMAL())
    doTest("ds<", before, after, Mode.NORMAL())
  }

  @Test
  fun testDeleteSurroundingTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}Foo</div>"

    doTest(listOf("dst"), before, after, Mode.NORMAL())
  }

  // VIM-1085
  @Test
  fun testDeleteSurroundingParamsAtLineEnd() {
    val before = "Foo\n" + "Seq(\"-${c}Yrangepos\")\n"
    val after = "Foo\n" + "Seq\"-Yrangepos\"\n"

    doTest(listOf("dsb"), before, after, Mode.NORMAL())
  }

  // VIM-1085
  @Test
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

    doTest(listOf("dsb"), before, after, Mode.NORMAL())
  }

  // VIM-2227
  @Test
  fun testDeleteInvalidSurroundingCharacter() {
    val text = "if (${c}condition) {"

    doTest("yibds]", text, text, Mode.NORMAL())
    doTest("yibds[", text, text, Mode.NORMAL())
    doTest("yibds}", text, text, Mode.NORMAL())
    doTest("yibds{", text, text, Mode.NORMAL())
  }

  @Test
  fun testRepeatDeleteSurroundParens() {
    val before = "if ((${c}condition)) {\n}\n"
    val after = "if condition {\n}\n"

    doTest(listOf("dsb."), before, after, Mode.NORMAL())
  }

  @Test
  fun testRepeatDeleteSurroundQuotes() {
    val before = "if (\"${c}condition\") {\n}\n"
    val after = "if (condition) {\n}\n"

    doTest(listOf("ds\"."), before, after, Mode.NORMAL())
  }

  /* Change surroundings */

  @Test
  fun testChangeSurroundingParens() {
    val before = "if (${c}condition) {\n" + "}\n"
    val after = "if [condition] {\n" + "}\n"

    doTest(listOf("csbr"), before, after, Mode.NORMAL())
  }

  @Test
  fun testChangeSurroundingEmptyParens() {
    doTest(listOf("cs)}"), "${c}()", "${c}{}", Mode.NORMAL())
    doTest(listOf("cs)}"), "(${c})", "${c}{}", Mode.NORMAL())
  }

  @Test
  fun testChangeSurroundingBlock() {
    val before = "if (condition) {${c}return;}"
    val after = "if (condition) (return;)"

    doTest(listOf("csBb"), before, after, Mode.NORMAL())
  }

  @Test
  fun testChangeSurroundingTagSimple() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}(Foo)</div>"

    doTest(listOf("cstb"), before, after, Mode.NORMAL())
  }

  @Test
  fun testChangeSurroundingTagAnotherTag() {
    val before = "<div><p>${c}Foo</p></div>"
    val after = "<div>${c}<b>Foo</b></div>"

    doTest(listOf("cst<b>"), before, after, Mode.NORMAL())
  }

  @Test
  fun testRepeatChangeSurroundingParens() {
    val before = "foo(${c}index)(index2) = bar;"
    val after = "foo[index][index2] = bar;"

    doTest(listOf("csbrE."), before, after, Mode.NORMAL())
  }

  // VIM-2227
  @Test
  fun testChangeInvalidSurroundingCharacter() {
    val text = "if (${c}condition) {"

    doTest("yibcs]}", text, text, Mode.NORMAL())
    doTest("yibcs[}", text, text, Mode.NORMAL())
    doTest("yibcs}]", text, text, Mode.NORMAL())
    doTest("yibcs{]", text, text, Mode.NORMAL())
  }

  @VimBehaviorDiffers(
    """
      <h1>Title</h1>
      
      <p>
      SurroundThis
      </p>
      
      <p>Some text</p>
  """,
  )
  @Test
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
      
      <p>
      SurroundThis
      </p>
      
      <p>Some text</p>
      """.trimIndent(),
      Mode.NORMAL(),
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
  """,
  )
  @Test
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
      <p>
          Surround This
      </p>
          <p>Some other paragraph</p>
      </div>
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
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
      Mode.NORMAL(),
    )
  }

  @Test
  fun testWithAnExistingMapping() {
    val before = "(foo)"
    val after = "[foo]"

    configureByText(before)

    typeText(commandToKeys("noremap d <C-d>"))
    typeText(injector.parser.parseKeys("cs(]"))
    assertState(after)
  }

  @Test
  fun `test change new line`() {
    val before = """
      "\n"
    """.trimIndent()
    configureByText(before)

    typeText(injector.parser.parseKeys("cs\"'"))
    val after = """'\n'"""
    assertState(after)
  }

  @Test
  fun testMappingSurroundPlugin() {
    val before = "if (condition) ${c}return;\n"
    val after = "if (condition) \"return\";\n"

    doTest(":map gw ysiw\"<CR>gw", before, after, Mode.NORMAL())
  }

  @Test
  fun testSurroundPluginWithMacro() {
    val before = """
      if (con${c}dition) return;
      if (condition) return;
    """.trimIndent()
    val after = """
      if condition return;
      if ${c}condition return;
    """.trimIndent()

    doTest("qqds)qj@q", before, after, Mode.NORMAL())
  }

  @Test
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
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test change surround with multicaret`() {
    val before = """
                  (${c}abc)
                  (${c}xyz)
                    """
    val after = """
                  ${c}[abc]
                  ${c}[xyz]
                    """

    doTest(listOf("cs(]"), before, after, Mode.NORMAL())
  }

  @Test
  fun `test delete surround with multicaret`() {
    val before = """
                  (${c}abc)
                  (${c}xyz)
                    """
    val after = """
                  ${c}abc
                  ${c}xyz
                    """

    doTest(listOf("ds("), before, after, Mode.NORMAL())
  }

  @Test
  fun `test surround line`() {
    val before = """
                  ${c}abc  
                  ${c}xyz  
                    """
    val after = """
                  ${c}"abc"  
                  ${c}"xyz"  
                    """

    doTest(listOf("yss\""), before, after, Mode.NORMAL())
  }

  @Test
  fun `test csw`() {
    val before = "var1, va${c}r2, var3"
    val after = "var1, ${c}\"var2\", var3"
    doTest(listOf("csw\""), before, after, Mode.NORMAL())
  }

  @Test
  fun `test surround does not remove unnecessary chars`() {
    val before = """
      <a id="languageChanger" class="shababMallFont" href="?lang={{ App::getLocale() == "en" ? "a${c}r" : "en" }}">
    """
    val after = """
      <a id="languageChanger" class="shababMallFont" href="?lang={{ App::getLocale() == "en" ? ${c}'ar' : "en" }}">
    """
    doTest(listOf("cs\"'"), before, after, Mode.NORMAL())
  }

  @Test
  fun `test surround does not remove unnecessary chars 2`() {
    val before = """
      "Hello "H${c}I test test" extra information"
    """
    val after = """
      "Hello (HI test test) extra information"
    """
    doTest(listOf("cs\")"), before, after, Mode.NORMAL())
  }

  // VIM-1824
  @ParameterizedTest(name = "testRemoveWhiteSpaceWithClosingBracket for ({0}, {1}, {2})")
  @MethodSource("removeWhiteSpaceWithClosingBracketParams")
  fun testRemoveWhiteSpaceWithClosingBracket(before: String, after: String, motion: String) {
    doTest(listOf(motion), before, after, Mode.NORMAL())
  }

  // VIM-3841
  @Test
  fun `test return to Normal mode after surround in Visual mode`() {
    doTest(
      listOf("veS\"", "i"),
      "lorem ${c}ipsum dolor sit amet",
      "lorem ${c}\"ipsum\" dolor sit amet",
      Mode.INSERT,
    )
  }

  companion object {
    @JvmStatic
    fun removeWhiteSpaceWithClosingBracketParams() = listOf(
      arrayOf("{ ${c}example }", "${c}{example}", "cs{}"),
      arrayOf("( ${c}example )", "${c}(example)", "cs()"),
      arrayOf("[ ${c}example ]", "${c}[example]", "cs[]"),

      // mutliple surrounding spaces are trimmed at once
      arrayOf("[  ${c}example  ]", "${c}[example]", "cs[]"),
      arrayOf("[${c}  ]", "${c}[]", "cs[]"),
      arrayOf("[${c} ]", "${c}[]", "cs[]"),

      // asymetric spaces are also trimmed at once
      arrayOf("[ ${c}example]", "${c}[example]", "cs[]"),
      arrayOf("[   ${c}example ]", "${c}[example]", "cs[]"),

      // empty brackets are not removed
      arrayOf("[${c}]", "${c}[]", "cs[]"),
    )
  }
}
