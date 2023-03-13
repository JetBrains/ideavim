/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.extension.argtextobj

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import java.util.*

@Suppress("SpellCheckingInspection")
class VimArgTextObjExtensionTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("argtextobj")
  }

  private fun setArgTextObjPairsVariable(value: String) {
    injector.vimscriptExecutor.execute("let argtextobj_pairs='$value'", true)
  }

  fun testDeleteAnArgument() {
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1<caret>)",
      "function(<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testChangeInnerArgument() {
    doTest(
      Lists.newArrayList("cia"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <caret>)",
      VimStateMachine.Mode.INSERT,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testSmartArgumentRecognition() {
    doTest(
      Lists.newArrayList("dia"),
      "function(1, (20<caret>*30)+40, somefunc2(3, 4))",
      "function(1, <caret>, somefunc2(3, 4))",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(1, (20*30)+40, somefunc2(<caret>3, 4))",
      "function(1, (20*30)+40, somefunc2(<caret>4))",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testIgnoreQuotedArguments() {
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* arg2=a,b,c(<caret>arg,e))",
      "function(int arg1,    char* arg2=a,b,c(<caret>e))",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* arg2=\"a,b,c(<caret>arg,e)\")",
      "function(int arg1<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* arg2=\"a,b,c(arg,e\"<caret>)",
      "function(int arg1<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int arg1,    char* a<caret>rg2={\"a,b},c(arg,e\"})",
      "function(int arg1<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testDeleteTwoArguments() {
    doTest(
      Lists.newArrayList("d2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
      "function(<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("d2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
      "function(<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("d2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<caret>bool arg3)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("d2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<caret>, bool arg3)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("d2aa"),
      "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
      "function(int arg1<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("d2ia"),
      "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
      "function(int arg1,    <caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testSelectTwoArguments() {
    doTest(
      Lists.newArrayList("v2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\", </selection>bool arg3)",
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
    doTest(
      Lists.newArrayList("v2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\"</selection>, bool arg3)",
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
  }

  fun testArgumentsInsideAngleBrackets() {
    setArgTextObjPairsVariable("(:),<:>")
    doTest(
      Lists.newArrayList("dia"),
      "std::vector<int, std::unique_p<caret>tr<bool>> v{};",
      "std::vector<int, <caret>> v{};",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testWhenUnbalancedHigherPriorityPairIsUsed() {
    setArgTextObjPairsVariable("{:},(:)")
    doTest(
      Lists.newArrayList("dia"),
      "namespace foo { void foo(int arg1, bool arg2<caret> { body }\n}",
      "namespace foo { void foo(int arg1, <caret>}",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("dia"),
      "namespace foo { void foo(int <caret>arg1, bool arg2 { body }\n}",
      "namespace foo { <caret>, bool arg2 { body }\n}",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testBracketPriorityToHangleShiftOperators() {
    doTest(
      Lists.newArrayList("dia"),
      "foo(30 << 10, 20 << <caret>3) >> 17",
      "foo(30 << 10, <caret>) >> 17",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("dia"),
      "foo(30 << <caret>10, 20 * 3) >> 17",
      "foo(<caret>, 20 * 3) >> 17",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("dia"),
      "foo(<caret>30 >> 10, 20 * 3) << 17",
      "foo(<caret>, 20 * 3) << 17",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testEmptyFile() {
    assertPluginError(false)
    doTest(
      Lists.newArrayList("daa"),
      "<caret>",
      "<caret>",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "<caret>",
      "<caret>",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
  }

  fun testEmptyLine() {
    assertPluginError(false)
    doTest(
      Lists.newArrayList("daa"),
      "<caret>\n",
      "<caret>\n",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "<caret>\n",
      "<caret>\n",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
  }

  fun testEmptyArg() {
    assertPluginError(false)
    doTest(
      Lists.newArrayList("daa"),
      "foo(<caret>)",
      "foo(<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "foo(<caret>)",
      "foo(<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
  }

  fun testSkipCommasInsideNestedPairs() {
    val before = """void foo(int arg1)
{   methodCall(arg1, "{ arg1 , 2");
   otherMeth<caret>odcall(arg, 3);
}"""
    doTest(Lists.newArrayList("dia"), before, before, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertPluginError(true)
  }

  fun testHandleNestedPairs() {
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, arr<caret>ay[someexpr(Class{arg1 << 3, arg2})] + 3)\n{",
      "foo(arg1, <caret>)\n{",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testHandleNestedParenthesisForASingleArgument() {
    doTest(
      Lists.newArrayList("dia"),
      "foo((20*<caret>30))",
      "foo(<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testHandleImbalancedPairs() {
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, ba<caret>r(not-an-arg{body",
      "foo(arg1, ba<caret>r(not-an-arg{body",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, ba<caret>r ( x > 3 )",
      "foo(arg1, ba<caret>r ( x > 3 )",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "foo(arg1, ba<caret>r + x >",
      "foo(arg1, ba<caret>r + x >",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    doTest(
      Lists.newArrayList("dia"),
      "<arg1, ba<caret>r + x)",
      "<arg1, ba<caret>r + x)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
  }

  fun testArgumentBoundsSearchIsLimitedByLineCount() {
    val before = """foo(
${java.lang.String.join("", Collections.nCopies(10, "   arg,\n"))}   last<caret>Arg)"""
    doTest(Lists.newArrayList("dia"), before, before, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    assertPluginError(true)
  }

  fun testExtendVisualSelection() {
    doTest(
      Lists.newArrayList("vllia"),
      "function(int arg1,    ch<caret>ar* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
    doTest(
      Lists.newArrayList("vhhia"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
  }

  fun testExtendVisualSelectionUsesCaretPos() {
    doTest(
      Lists.newArrayList("vllia"),
      "fu<caret>n(arg)",
      "fun(<selection>arg</selection>)",
      VimStateMachine.Mode.VISUAL,
      VimStateMachine.SubMode.VISUAL_CHARACTER,
    )
  }

  fun testDeleteArrayArgument() {
    setArgTextObjPairsVariable("[:],(:)")
    doTest(
      Lists.newArrayList("dia"),
      "function(int a, String[<caret>] b)",
      "function(int a, <caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function(int a, String[<caret>] b)",
      "function(int a)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testDeleteInClass() {
    doTest(
      Lists.newArrayList("dia"),
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      "class MyClass{ public int myFun() { some<caret>Call(); } }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testFunctionWithSpaceAfterName() {
    doTest(
      Lists.newArrayList("dia"),
      "function (int <caret>a)",
      "function (int <caret>a)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function (int <caret>a)",
      "function (int <caret>a)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @VimBehaviorDiffers(
    originalVimAfter = "function (int <caret>a, int b)",
    description = "Should work the same as testFunctionWithSpaceAfterName",
  )
  fun testFunctionWithSpaceAfterNameWithTwoArgs() {
    doTest(
      Lists.newArrayList("dia"),
      "function (int <caret>a, int b)",
      "function (, int b)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "function (int <caret>a, int b)",
      "function (int b)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testDeleteInIf() {
    doTest(
      Lists.newArrayList("dia"),
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  fun testParseVariablePairs() {
    assertPluginError(false)
    setArgTextObjPairsVariable("[:], (:)")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("expecting ':', but got '(' instead")
    setArgTextObjPairsVariable("[:](:)")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("expecting ',', but got '(' instead")
    setArgTextObjPairsVariable("=:=")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("open and close brackets must be different")
    setArgTextObjPairsVariable("[:],(:")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("list of pairs is incomplete")
    setArgTextObjPairsVariable("")
    doTest(
      Lists.newArrayList("daa"),
      "f(a<caret>)",
      "f(a<caret>)",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(true)
    assertPluginErrorMessageContains("list of pairs is incomplete")
    setArgTextObjPairsVariable("[:],(:)")
    doTest(
      Lists.newArrayList("daa"),
      "f[a<caret>]",
      "f[<caret>]",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(false)
    setArgTextObjPairsVariable("::;")
    doTest(
      Lists.newArrayList("daa"),
      "f: a<caret> ;",
      "f:<caret>;",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    assertPluginError(false)
  }

  fun testCppLambaArguments() {
    setArgTextObjPairsVariable("[:],(:),{:},<:>")
    doTest(
      Lists.newArrayList("daa"),
      "[capture1, c = <caret>capture2] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
      "[capture1] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "[capture1, c = capture2] { return Clazz<int,<caret> bool>{ctorParam1, ctorParam2}; }",
      "[capture1, c = capture2] { return Clazz<int>{ctorParam1, ctorParam2}; }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
    doTest(
      Lists.newArrayList("daa"),
      "[capture1, c = capture2] { return Clazz<int, bool>{ctorPar<caret>am1, ctorParam2}; }",
      "[capture1, c = capture2] { return Clazz<int, bool>{ctorParam2}; }",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }
}
