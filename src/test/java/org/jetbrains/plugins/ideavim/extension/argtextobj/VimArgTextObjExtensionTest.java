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

package org.jetbrains.plugins.ideavim.extension.argtextobj;

import com.google.common.collect.Lists;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.VimStateMachine;
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.Collections;


public class VimArgTextObjExtensionTest extends VimTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableExtensions("argtextobj");
  }

  private void setArgTextObjPairsVariable(String value) {
    VimInjectorKt.getInjector().getVimscriptExecutor().execute("let argtextobj_pairs='" + value + "'", true);
  }


  public void testDeleteAnArgument() {
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1<caret>)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1<caret>)", "function(<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testChangeInnerArgument() {
    doTest(Lists.newArrayList("cia"), "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <caret>)", VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE);
  }

  public void testSmartArgumentRecognition() {
    doTest(Lists.newArrayList("dia"), "function(1, (20<caret>*30)+40, somefunc2(3, 4))",
           "function(1, <caret>, somefunc2(3, 4))", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(1, (20*30)+40, somefunc2(<caret>3, 4))",
           "function(1, (20*30)+40, somefunc2(<caret>4))", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIgnoreQuotedArguments() {
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* arg2=a,b,c(<caret>arg,e))",
           "function(int arg1,    char* arg2=a,b,c(<caret>e))", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* arg2=\"a,b,c(<caret>arg,e)\")",
           "function(int arg1<caret>)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* arg2=\"a,b,c(arg,e\"<caret>)",
           "function(int arg1<caret>)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* a<caret>rg2={\"a,b},c(arg,e\"})",
           "function(int arg1<caret>)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDeleteTwoArguments() {
    doTest(Lists.newArrayList("d2aa"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")", "function(<caret>)",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("d2ia"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")", "function(<caret>)",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("d2aa"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<caret>bool arg3)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("d2ia"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<caret>, bool arg3)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("d2aa"), "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
           "function(int arg1<caret>)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("d2ia"), "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
           "function(int arg1,    <caret>)", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testSelectTwoArguments() {
    doTest(Lists.newArrayList("v2aa"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\", </selection>bool arg3)",
           VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER);
    doTest(Lists.newArrayList("v2ia"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\"</selection>, bool arg3)",
           VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER);
  }

  public void testArgumentsInsideAngleBrackets() {
    setArgTextObjPairsVariable("(:),<:>");
    doTest(Lists.newArrayList("dia"), "std::vector<int, std::unique_p<caret>tr<bool>> v{};",
           "std::vector<int, <caret>> v{};", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testWhenUnbalancedHigherPriorityPairIsUsed() {
    setArgTextObjPairsVariable("{:},(:)");
    doTest(Lists.newArrayList("dia"), "namespace foo { void foo(int arg1, bool arg2<caret> { body }\n}",
           "namespace foo { void foo(int arg1, <caret>}", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("dia"), "namespace foo { void foo(int <caret>arg1, bool arg2 { body }\n}",
           "namespace foo { <caret>, bool arg2 { body }\n}", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testBracketPriorityToHangleShiftOperators() {
    doTest(Lists.newArrayList("dia"), "foo(30 << 10, 20 << <caret>3) >> 17", "foo(30 << 10, <caret>) >> 17",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("dia"), "foo(30 << <caret>10, 20 * 3) >> 17", "foo(<caret>, 20 * 3) >> 17",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("dia"), "foo(<caret>30 >> 10, 20 * 3) << 17", "foo(<caret>, 20 * 3) << 17",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testEmptyFile() {
    assertPluginError(false);
    doTest(Lists.newArrayList("daa"), "<caret>", "<caret>", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "<caret>", "<caret>", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
  }

  public void testEmptyLine() {
    assertPluginError(false);
    doTest(Lists.newArrayList("daa"), "<caret>\n", "<caret>\n", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "<caret>\n", "<caret>\n", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
  }

  public void testEmptyArg() {
    assertPluginError(false);
    doTest(Lists.newArrayList("daa"), "foo(<caret>)", "foo(<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "foo(<caret>)", "foo(<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
  }

  public void testSkipCommasInsideNestedPairs() {
    final String before =
      "void foo(int arg1)\n{" + "   methodCall(arg1, \"{ arg1 , 2\");\n" + "   otherMeth<caret>odcall(arg, 3);\n" + "}";
    doTest(Lists.newArrayList("dia"), before, before, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
  }

  public void testHandleNestedPairs() {
    doTest(Lists.newArrayList("dia"), "foo(arg1, arr<caret>ay[someexpr(Class{arg1 << 3, arg2})] + 3)\n{",
           "foo(arg1, <caret>)\n{", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testHandleNestedParenthesisForASingleArgument() {
    doTest(Lists.newArrayList("dia"), "foo((20*<caret>30))", "foo(<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testHandleImbalancedPairs() {
    doTest(Lists.newArrayList("dia"), "foo(arg1, ba<caret>r(not-an-arg{body", "foo(arg1, ba<caret>r(not-an-arg{body",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "foo(arg1, ba<caret>r ( x > 3 )", "foo(arg1, ba<caret>r ( x > 3 )",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "foo(arg1, ba<caret>r + x >", "foo(arg1, ba<caret>r + x >",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "<arg1, ba<caret>r + x)", "<arg1, ba<caret>r + x)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
  }

  public void testArgumentBoundsSearchIsLimitedByLineCount() {
    final String before = "foo(\n" + String.join("", Collections.nCopies(10, "   arg,\n")) + "   last<caret>Arg" + ")";
    doTest(Lists.newArrayList("dia"), before, before, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    assertPluginError(true);
  }

  public void testExtendVisualSelection() {
    doTest(Lists.newArrayList("vllia"), "function(int arg1,    ch<caret>ar* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)", VimStateMachine.Mode.VISUAL,
           VimStateMachine.SubMode.VISUAL_CHARACTER);
    doTest(Lists.newArrayList("vhhia"), "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)", VimStateMachine.Mode.VISUAL,
           VimStateMachine.SubMode.VISUAL_CHARACTER);
  }

  public void testExtendVisualSelectionUsesCaretPos() {
    doTest(Lists.newArrayList("vllia"), "fu<caret>n(arg)", "fun(<selection>arg</selection>)", VimStateMachine.Mode.VISUAL,
           VimStateMachine.SubMode.VISUAL_CHARACTER);
  }

  public void testDeleteArrayArgument() {
    setArgTextObjPairsVariable("[:],(:)");
    doTest(Lists.newArrayList("dia"), "function(int a, String[<caret>] b)", "function(int a, <caret>)",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int a, String[<caret>] b)", "function(int a)",
           VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDeleteInClass() {
    doTest(Lists.newArrayList("dia"), "class MyClass{ public int myFun() { some<caret>Call(); } }",
           "class MyClass{ public int myFun() { some<caret>Call(); } }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "class MyClass{ public int myFun() { some<caret>Call(); } }",
           "class MyClass{ public int myFun() { some<caret>Call(); } }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testFunctionWithSpaceAfterName() {
    doTest(Lists.newArrayList("dia"), "function (int <caret>a)", "function (int <caret>a)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function (int <caret>a)", "function (int <caret>a)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  @VimBehaviorDiffers(originalVimAfter = "function (int <caret>a, int b)", description = "Should work the same as testFunctionWithSpaceAfterName")
  public void testFunctionWithSpaceAfterNameWithTwoArgs() {
    doTest(Lists.newArrayList("dia"), "function (int <caret>a, int b)", "function (, int b)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function (int <caret>a, int b)", "function (int b)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testDeleteInIf() {
    doTest(Lists.newArrayList("dia"), "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testParseVariablePairs() {
    assertPluginError(false);
    setArgTextObjPairsVariable("[:], (:)");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("expecting ':', but got '(' instead");

    setArgTextObjPairsVariable("[:](:)");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("expecting ',', but got '(' instead");

    setArgTextObjPairsVariable("=:=");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("open and close brackets must be different");

    setArgTextObjPairsVariable("[:],(:");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("list of pairs is incomplete");

    setArgTextObjPairsVariable("");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("list of pairs is incomplete");

    setArgTextObjPairsVariable("[:],(:)");
    doTest(Lists.newArrayList("daa"), "f[a<caret>]", "f[<caret>]", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(false);

    setArgTextObjPairsVariable("::;");
    doTest(Lists.newArrayList("daa"), "f: a<caret> ;", "f:<caret>;", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    assertPluginError(false);

  }

  public void testCppLambaArguments() {
    setArgTextObjPairsVariable("[:],(:),{:},<:>");
    doTest(Lists.newArrayList("daa"),
           "[capture1, c = <caret>capture2] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
           "[capture1] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"),
           "[capture1, c = capture2] { return Clazz<int,<caret> bool>{ctorParam1, ctorParam2}; }",
           "[capture1, c = capture2] { return Clazz<int>{ctorParam1, ctorParam2}; }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
    doTest(Lists.newArrayList("daa"),
           "[capture1, c = capture2] { return Clazz<int, bool>{ctorPar<caret>am1, ctorParam2}; }",
           "[capture1, c = capture2] { return Clazz<int, bool>{ctorParam2}; }", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

}
