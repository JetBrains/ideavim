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

package org.jetbrains.plugins.ideavim.extension.argtextobj;

import com.google.common.collect.Lists;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.Collections;

public class VimArgTextObjExtensionTest extends VimTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableExtensions("argtextobj");
  }

  private void setArgTextObjPairsVariable(String value) {
    VimScriptGlobalEnvironment.getInstance().getVariables().put("g:argtextobj_pairs", value);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testDeleteAnArgument() {
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1<caret>)", "function(<caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testChangeInnerArgument() {
    doTest(Lists.newArrayList("cia"), "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <caret>)", CommandState.Mode.INSERT, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testSmartArgumentRecognition() {
    doTest(Lists.newArrayList("dia"), "function(1, (20<caret>*30)+40, somefunc2(3, 4))",
           "function(1, <caret>, somefunc2(3, 4))", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(1, (20*30)+40, somefunc2(<caret>3, 4))",
           "function(1, (20*30)+40, somefunc2(<caret>4))", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testIgnoreQuotedArguments() {
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* arg2=a,b,c(<caret>arg,e))",
           "function(int arg1,    char* arg2=a,b,c(<caret>e))", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* arg2=\"a,b,c(<caret>arg,e)\")",
           "function(int arg1<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* arg2=\"a,b,c(arg,e\"<caret>)",
           "function(int arg1<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int arg1,    char* a<caret>rg2={\"a,b},c(arg,e\"})",
           "function(int arg1<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testDeleteTwoArguments() {
    doTest(Lists.newArrayList("d2aa"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
           "function(<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("d2ia"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
           "function(<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("d2aa"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<caret>bool arg3)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("d2ia"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<caret>, bool arg3)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("d2aa"), "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
           "function(int arg1<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("d2ia"), "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
           "function(int arg1,    <caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testSelectTwoArguments() {
    doTest(Lists.newArrayList("v2aa"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\", </selection>bool arg3)",
           CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
    doTest(Lists.newArrayList("v2ia"), "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
           "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\"</selection>, bool arg3)",
           CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testArgumentsInsideAngleBrackets() {
    setArgTextObjPairsVariable("(:),<:>");
    doTest(Lists.newArrayList("dia"), "std::vector<int, std::unique_p<caret>tr<bool>> v{};",
           "std::vector<int, <caret>> v{};", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testWhenUnbalancedHigherPriorityPairIsUsed() {
    setArgTextObjPairsVariable("{:},(:)");
    doTest(Lists.newArrayList("dia"), "namespace foo { void foo(int arg1, bool arg2<caret> { body }\n}",
           "namespace foo { void foo(int arg1, <caret>}", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("dia"), "namespace foo { void foo(int <caret>arg1, bool arg2 { body }\n}",
           "namespace foo { <caret>, bool arg2 { body }\n}", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testBracketPriorityToHangleShiftOperators() {
    doTest(Lists.newArrayList("dia"), "foo(30 << 10, 20 << <caret>3) >> 17", "foo(30 << 10, <caret>) >> 17",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("dia"), "foo(30 << <caret>10, 20 * 3) >> 17", "foo(<caret>, 20 * 3) >> 17",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("dia"), "foo(<caret>30 >> 10, 20 * 3) << 17", "foo(<caret>, 20 * 3) << 17",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testEmptyFile() {
    assertPluginError(false);
    doTest(Lists.newArrayList("daa"), "<caret>", "<caret>", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "<caret>", "<caret>", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testEmptyLine() {
    assertPluginError(false);
    doTest(Lists.newArrayList("daa"), "<caret>\n", "<caret>\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "<caret>\n", "<caret>\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testEmptyArg() {
    assertPluginError(false);
    doTest(Lists.newArrayList("daa"), "foo(<caret>)", "foo(<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "foo(<caret>)", "foo(<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testSkipCommasInsideNestedPairs() {
    final String before =
      "void foo(int arg1)\n{" + "   methodCall(arg1, \"{ arg1 , 2\");\n" + "   otherMeth<caret>odcall(arg, 3);\n" + "}";
    doTest(Lists.newArrayList("dia"), before, before, CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testHandleNestedPairs() {
    doTest(Lists.newArrayList("dia"), "foo(arg1, arr<caret>ay[someexpr(Class{arg1 << 3, arg2})] + 3)\n{",
           "foo(arg1, <caret>)\n{", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testHandleNestedParenthesisForASingleArgument() {
    doTest(Lists.newArrayList("dia"), "foo((20*<caret>30))", "foo(<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testHandleImbalancedPairs() {
    doTest(Lists.newArrayList("dia"), "foo(arg1, ba<caret>r(not-an-arg{body",
           "foo(arg1, ba<caret>r(not-an-arg{body", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "foo(arg1, ba<caret>r ( x > 3 )", "foo(arg1, ba<caret>r ( x > 3 )",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "foo(arg1, ba<caret>r + x >", "foo(arg1, ba<caret>r + x >",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(Lists.newArrayList("dia"), "<arg1, ba<caret>r + x)", "<arg1, ba<caret>r + x)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testArgumentBoundsSearchIsLimitedByLineCount() {
    final String before = "foo(\n" + String.join("", Collections.nCopies(10, "   arg,\n")) + "   last<caret>Arg" + ")";
    doTest(Lists.newArrayList("dia"), before, before, CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testExtendVisualSelection() {
    doTest(Lists.newArrayList("vllia"), "function(int arg1,    ch<caret>ar* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
           CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
    doTest(Lists.newArrayList("vhhia"), "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
           CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testExtendVisualSelectionUsesCaretPos() {
    doTest(Lists.newArrayList("vllia"), "fu<caret>n(arg)", "fun(<selection>arg</selection>)",
           CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testDeleteArrayArgument() {
    setArgTextObjPairsVariable("[:],(:)");
    doTest(Lists.newArrayList("dia"), "function(int a, String[<caret>] b)", "function(int a, <caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function(int a, String[<caret>] b)", "function(int a)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testDeleteInClass() {
    doTest(Lists.newArrayList("dia"), "class MyClass{ public int myFun() { some<caret>Call(); } }",
           "class MyClass{ public int myFun() { some<caret>Call(); } }", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "class MyClass{ public int myFun() { some<caret>Call(); } }",
           "class MyClass{ public int myFun() { some<caret>Call(); } }", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testFunctionWithSpaceAfterName() {
    doTest(Lists.newArrayList("dia"), "function (int <caret>a)", "function (int <caret>a)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function (int <caret>a)", "function (int <caret>a)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @VimBehaviorDiffers(originalVimAfter = "function (int <caret>a, int b)", description = "Should work the same as testFunctionWithSpaceAfterName")
  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testFunctionWithSpaceAfterNameWithTwoArgs() {
    doTest(Lists.newArrayList("dia"), "function (int <caret>a, int b)", "function (, int b)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"), "function (int <caret>a, int b)", "function (int b)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testDeleteInIf() {
    doTest(Lists.newArrayList("dia"),
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"),
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testParseVariablePairs() {
    assertPluginError(false);
    setArgTextObjPairsVariable("[:], (:)");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("expecting ':', but got '(' instead");

    setArgTextObjPairsVariable("[:](:)");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("expecting ',', but got '(' instead");

    setArgTextObjPairsVariable("=:=");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("open and close brackets must be different");

    setArgTextObjPairsVariable("[:],(:");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("list of pairs is incomplete");

    setArgTextObjPairsVariable("");
    doTest(Lists.newArrayList("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("list of pairs is incomplete");

    setArgTextObjPairsVariable("[:],(:)");
    doTest(Lists.newArrayList("daa"), "f[a<caret>]", "f[<caret>]", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(false);

    setArgTextObjPairsVariable("::;");
    doTest(Lists.newArrayList("daa"), "f: a<caret> ;", "f:<caret>;", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    assertPluginError(false);

  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  public void testCppLambaArguments() {
    setArgTextObjPairsVariable("[:],(:),{:},<:>");
    doTest(Lists.newArrayList("daa"),
           "[capture1, c = <caret>capture2] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
           "[capture1] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"),
           "[capture1, c = capture2] { return Clazz<int,<caret> bool>{ctorParam1, ctorParam2}; }",
           "[capture1, c = capture2] { return Clazz<int>{ctorParam1, ctorParam2}; }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(Lists.newArrayList("daa"),
           "[capture1, c = capture2] { return Clazz<int, bool>{ctorPar<caret>am1, ctorParam2}; }",
           "[capture1, c = capture2] { return Clazz<int, bool>{ctorParam2}; }", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

}
