/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.extension.argtextobj

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.util.*

@Suppress("SpellCheckingInspection")
class VimArgTextObjExtensionTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
    enableExtensions("argtextobj")
  }

  private fun setArgTextObjPairsVariable(value: String) {
    executeVimscript("let argtextobj_pairs='$value'", true)
  }

  @Test
  fun testDeleteAnArgument() {
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1<caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1<caret>)",
      "function(<caret>)",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testChangeInnerArgument() {
    doTest(
      Lists.newArrayList("cia"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <caret>)",
      Mode.INSERT,
    )
  }

  @Test
  fun testSmartArgumentRecognition() {
    doTest(
      Lists.newArrayList("dia"),
      "function(1, (20<caret>*30)+40, somefunc2(3, 4))",
      "function(1, <caret>, somefunc2(3, 4))",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(1, (20*30)+40, somefunc2(<caret>3, 4))",
      "function(1, (20*30)+40, somefunc2(<caret>4))",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIgnoreQuotedArguments() {
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* arg2=a,b,c(<caret>arg,e))",
      "function(int arg1,    char* arg2=a,b,c(<caret>e))",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* arg2=\"a,b,c(<caret>arg,e)\")",
      "function(int arg1<caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* arg2=\"a,b,c(arg,e\"<caret>)",
      "function(int arg1<caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* a<caret>rg2={\"a,b},c(arg,e\"})",
      "function(int arg1<caret>)",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteTwoArguments() {
    doTest(
      Lists.newArrayList("d2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
      "function(<caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("d2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
      "function(<caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("d2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<caret>bool arg3)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("d2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<caret>, bool arg3)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("d2aa"),
      "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
      "function(int arg1<caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("d2ia"),
      "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
      "function(int arg1,    <caret>)",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testSelectTwoArguments() {
    doTest(
      Lists.newArrayList("v2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\", </selection>bool arg3)",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
    doTest(
      Lists.newArrayList("v2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\"</selection>, bool arg3)",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun testArgumentsInsideAngleBrackets() {
    setArgTextObjPairsVariable("(:),<:>")
    doTest(
      Lists.newArrayList("dia"),
      "std::vector<int, std::unique_p<caret>tr<bool>> v{};",
      "std::vector<int, <caret>> v{};",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testWhenUnbalancedHigherPriorityPairIsUsed() {
    setArgTextObjPairsVariable("{:},(:)")
    doTest(
      Lists.newArrayList("dia"),
      "namespace foo { void foo(int arg1, bool arg2<caret> { body }\n}",
      "namespace foo { void foo(int arg1, <caret>}",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("dia"),
      "namespace foo { void foo(int <caret>arg1, bool arg2 { body }\n}",
      "namespace foo { <caret>, bool arg2 { body }\n}",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testBracketPriorityToHangleShiftOperators() {
    doTest(
      Lists.newArrayList("dia"),
      "foo(30 << 10, 20 << <caret>3) >> 17",
      "foo(30 << 10, <caret>) >> 17",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("dia"),
      "foo(30 << <caret>10, 20 * 3) >> 17",
      "foo(<caret>, 20 * 3) >> 17",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("dia"),
      "foo(<caret>30 >> 10, 20 * 3) << 17",
      "foo(<caret>, 20 * 3) << 17",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testEmptyFile() {
    assertPluginError(false)
    doTest(
      Lists.newArrayList("daa"),
      "<caret>",
      "<caret>",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "<caret>",
      "<caret>",
      Mode.NORMAL(),
    )
    assertPluginError(true)
  }

  @Test
  fun testEmptyLine() {
    assertPluginError(false)
    doTest(
      Lists.newArrayList("daa"),
      "<caret>\n",
      "<caret>\n",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "<caret>\n",
      "<caret>\n",
      Mode.NORMAL(),
    )
    assertPluginError(true)
  }

  @Test
  fun testEmptyArg() {
    assertPluginError(false)
    doTest(
      Lists.newArrayList("daa"),
      "foo(<caret>)",
      "foo(<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "foo(<caret>)",
      "foo(<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
  }

  @Test
  fun testSkipCommasInsideNestedPairs() {
    val before = """void foo(int arg1)
{   methodCall(arg1, "{ arg1 , 2");
   otherMeth<caret>odcall(arg, 3);
}"""
    doTest(Lists.newArrayList("dia"), before, before, Mode.NORMAL())
    assertPluginError(true)
  }

  @Test
  fun testHandleNestedPairs() {
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, arr<caret>ay[someexpr(Class{arg1 << 3, arg2})] + 3)\n{",
      "foo(arg1, <caret>)\n{",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testHandleNestedParenthesisForASingleArgument() {
    doTest(
      Lists.newArrayList("dia"),
      "foo((20*<caret>30))",
      "foo(<caret>)",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testHandleImbalancedPairs() {
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, ba<caret>r(not-an-arg{body",
      "foo(arg1, ba<caret>r(not-an-arg{body",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, ba<caret>r ( x > 3 )",
      "foo(arg1, ba<caret>r ( x > 3 )",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, ba<caret>r + x >",
      "foo(arg1, ba<caret>r + x >",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "<arg1, ba<caret>r + x)",
      "<arg1, ba<caret>r + x)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
  }

  @Test
  fun testArgumentBoundsSearchIsLimitedByLineCount() {
    val before = """foo(
${java.lang.String.join("", Collections.nCopies(10, "   arg,\n"))}   last<caret>Arg)"""
    doTest(Lists.newArrayList("dia"), before, before, Mode.NORMAL())
    assertPluginError(true)
  }

  @Test
  fun testExtendVisualSelection() {
    doTest(
      Lists.newArrayList("vllia"),
      "function(int arg1,    ch<caret>ar* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
    doTest(
      Lists.newArrayList("vhhia"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun testExtendVisualSelectionUsesCaretPos() {
    doTest(
      Lists.newArrayList("vllia"),
      "fu<caret>n(arg)",
      "fun(<selection>arg</selection>)",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun testDeleteArrayArgument() {
    setArgTextObjPairsVariable("[:],(:)")
    doTest(
      Lists.newArrayList("dia"),
      "function(int a, String[<caret>] b)",
      "function(int a, <caret>)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int a, String[<caret>] b)",
      "function(int a)",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteInClass() {
    doTest(
      Lists.newArrayList("dia"),
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testFunctionWithSpaceAfterName() {
    doTest(
      Lists.newArrayList("dia"),
      "function (int <caret>a)",
      "function (int <caret>a)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function (int <caret>a)",
      "function (int <caret>a)",
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = "function (int <caret>a, int b)",
    description = "Should work the same as testFunctionWithSpaceAfterName",
  )
  @Test
  fun testFunctionWithSpaceAfterNameWithTwoArgs() {
    doTest(
      Lists.newArrayList("dia"),
      "function (int <caret>a, int b)",
      "function (, int b)",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "function (int <caret>a, int b)",
      "function (int b)",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDeleteInIf() {
    doTest(
      Lists.newArrayList("dia"),
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testParseVariablePairs() {
    assertPluginError(false)
    setArgTextObjPairsVariable("[:], (:)")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("expecting ':', but got '(' instead")
    setArgTextObjPairsVariable("[:](:)")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("expecting ',', but got '(' instead")
    setArgTextObjPairsVariable("=:=")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("open and close brackets must be different")
    setArgTextObjPairsVariable("[:],(:")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("list of pairs is incomplete")
    setArgTextObjPairsVariable("")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      Mode.NORMAL(),
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("list of pairs is incomplete")
    setArgTextObjPairsVariable("[:],(:)")
    doTest(
      Lists.newArrayList("daa"),
      "f[a<caret>]",
      "f[<caret>]",
      Mode.NORMAL(),
    )
    assertPluginError(false)
    setArgTextObjPairsVariable("::;")
    doTest(
      Lists.newArrayList("daa"),
      "f: a<caret> ;",
      "f:<caret>;",
      Mode.NORMAL(),
    )
    assertPluginError(false)
  }

  @Test
  fun testCppLambaArguments() {
    setArgTextObjPairsVariable("[:],(:),{:},<:>")
    doTest(
      Lists.newArrayList("daa"),
      "[capture1, c = <caret>capture2] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
      "[capture1] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "[capture1, c = capture2] { return Clazz<int,<caret> bool>{ctorParam1, ctorParam2}; }",
      "[capture1, c = capture2] { return Clazz<int>{ctorParam1, ctorParam2}; }",
      Mode.NORMAL(),
    )
    doTest(
      Lists.newArrayList("daa"),
      "[capture1, c = capture2] { return Clazz<int, bool>{ctorPar<caret>am1, ctorParam2}; }",
      "[capture1, c = capture2] { return Clazz<int, bool>{ctorParam2}; }",
      Mode.NORMAL(),
    )
  }
}
