package org.jetbrains.plugins.ideavim.extesion.argtextobj;

import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers;
import org.jetbrains.plugins.ideavim.VimTestCase;

import java.util.Collections;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class VimArgTextObjExtensionTest extends VimTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableExtensions("argtextobj");
  }

  private void setArgTextObjPairsVariable(String value) {
    VimScriptGlobalEnvironment.getInstance().getVariables().put("g:argtextobj_pairs", value);
  }

  public void testDeleteAnArgument() {
    doTest(parseKeys("daa"),
           "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1<caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "function(int arg1<caret>)",
           "function(<caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testChangeInnerArgument() {
    doTest(parseKeys("cia"),
           "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
           "function(int arg1,    <caret>)",
           CommandState.Mode.INSERT, CommandState.SubMode.NONE);
  }

  public void testSmartArgumentRecognition() {
    doTest(parseKeys("dia"),
           "function(1, (20<caret>*30)+40, somefunc2(3, 4))",
           "function(1, <caret>, somefunc2(3, 4))",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "function(1, (20*30)+40, somefunc2(<caret>3, 4))",
           "function(1, (20*30)+40, somefunc2(<caret>4))",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIgnoreQuotedArguments() {
    doTest(parseKeys("daa"),
           "function(int arg1,    char* arg2=a,b,c(<caret>arg,e))",
           "function(int arg1,    char* arg2=a,b,c(<caret>e))",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "function(int arg1,    char* arg2=\"a,b,c(<caret>arg,e)\")",
           "function(int arg1<caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
      "function(int arg1,    char* arg2=\"a,b,c(arg,e\"<caret>)",
      "function(int arg1<caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
      "function(int arg1,    char* a<caret>rg2={\"a,b},c(arg,e\"})",
      "function(int arg1<caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteTwoArguments() {
    doTest(parseKeys("d2aa"),
           "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
           "function(<caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("d2ia"),
           "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\")",
           "function(<caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("d2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<caret>bool arg3)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("d2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<caret>, bool arg3)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("d2aa"),
      "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
      "function(int arg1<caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("d2ia"),
      "function(int arg1,    char* arg<caret>2=\"a,b,c(d,e)\", bool arg3)",
      "function(int arg1,    <caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testSelectTwoArguments() {
    doTest(parseKeys("v2aa"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\", </selection>bool arg3)",
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
    doTest(parseKeys("v2ia"),
      "function(int <caret>arg1,    char* arg2=\"a,b,c(d,e)\", bool arg3)",
      "function(<selection>int arg1,    char* arg2=\"a,b,c(d,e)\"</selection>, bool arg3)",
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
  }

  public void testArgumentsInsideAngleBrackets() {
    setArgTextObjPairsVariable("(:),<:>");
    doTest(parseKeys("dia"),
           "std::vector<int, std::unique_p<caret>tr<bool>> v{};",
           "std::vector<int, <caret>> v{};",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testWhenUnbalancedHigherPriorityPairIsUsed() {
    setArgTextObjPairsVariable("{:},(:)");
    doTest(parseKeys("dia"),
      "namespace foo { void foo(int arg1, bool arg2<caret> { body }\n}",
      "namespace foo { void foo(int arg1, <caret>}",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("dia"),
      "namespace foo { void foo(int <caret>arg1, bool arg2 { body }\n}",
      "namespace foo { <caret>, bool arg2 { body }\n}",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testBracketPriorityToHangleShiftOperators() {
    doTest(parseKeys("dia"),
           "foo(30 << 10, 20 << <caret>3) >> 17",
           "foo(30 << 10, <caret>) >> 17",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("dia"),
           "foo(30 << <caret>10, 20 * 3) >> 17",
           "foo(<caret>, 20 * 3) >> 17",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("dia"),
      "foo(<caret>30 >> 10, 20 * 3) << 17",
      "foo(<caret>, 20 * 3) << 17",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testEmptyFile() {
    assertPluginError(false);
    doTest(parseKeys("daa"),
      "<caret>",
      "<caret>",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(parseKeys("dia"),
      "<caret>",
      "<caret>",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  public void testEmptyLine() {
    assertPluginError(false);
    doTest(parseKeys("daa"),
      "<caret>\n",
      "<caret>\n",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(parseKeys("dia"),
      "<caret>\n",
      "<caret>\n",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  public void testEmptyArg() {
    assertPluginError(false);
    doTest(parseKeys("daa"),
      "foo(<caret>)",
      "foo(<caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(parseKeys("dia"),
      "foo(<caret>)",
      "foo(<caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  public void testSkipCommasInsideNestedPairs() {
    final String before = "void foo(int arg1)\n{" +
      "   methodCall(arg1, \"{ arg1 , 2\");\n" +
      "   otherMeth<caret>odcall(arg, 3);\n" +
      "}";
    doTest(parseKeys("dia"), before, before,
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  public void testHandleNestedPairs() {
    doTest(parseKeys("dia"),
      "foo(arg1, arr<caret>ay[someexpr(Class{arg1 << 3, arg2})] + 3)\n{",
      "foo(arg1, <caret>)\n{",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testHandleNestedParenthesisForASingleArgument() {
    doTest(parseKeys("dia"),
      "foo((20*<caret>30))",
      "foo(<caret>)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testHandleImbalancedPairs() {
    doTest(parseKeys("dia"),
      "foo(arg1, ba<caret>r(not-an-arg{body",
      "foo(arg1, ba<caret>r(not-an-arg{body",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(parseKeys("dia"),
      "foo(arg1, ba<caret>r ( x > 3 )",
      "foo(arg1, ba<caret>r ( x > 3 )",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(parseKeys("dia"),
      "foo(arg1, ba<caret>r + x >",
      "foo(arg1, ba<caret>r + x >",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    doTest(parseKeys("dia"),
      "<arg1, ba<caret>r + x)",
      "<arg1, ba<caret>r + x)",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  public void testArgumentBoundsSearchIsLimitedByLineCount() {
    final String before = "foo(\n" +
      String.join("", Collections.nCopies(10, "   arg,\n")) +
      "   last<caret>Arg" +
      ")";
    doTest(parseKeys("dia"), before, before,
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
  }

  public void testExtendVisualSelection() {
    doTest(parseKeys("vllia"),
      "function(int arg1,    ch<caret>ar* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
    doTest(parseKeys("vhhia"),
      "function(int arg1,    char<caret>* arg2=\"a,b,c(d,e)\")",
      "function(int arg1,    <selection>char* arg2=\"a,b,c(d,e)\"</selection>)",
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
  }

  public void testExtendVisualSelectionUsesCaretPos() {
    doTest(parseKeys("vllia"),
      "fu<caret>n(arg)",
      "fun(<selection>arg</selection>)",
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER);
  }

  public void testDeleteArrayArgument() {
    setArgTextObjPairsVariable("[:],(:)");
    doTest(parseKeys("dia"),
           "function(int a, String[<caret>] b)",
           "function(int a, <caret>)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "function(int a, String[<caret>] b)",
           "function(int a)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteInClass() {
    doTest(parseKeys("dia"),
           "class MyClass{ public int myFun() { some<caret>Call(); } }",
           "class MyClass{ public int myFun() { some<caret>Call(); } }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "class MyClass{ public int myFun() { some<caret>Call(); } }",
           "class MyClass{ public int myFun() { some<caret>Call(); } }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testFunctionWithSpaceAfterName() {
    doTest(parseKeys("dia"),
           "function (int <caret>a)",
           "function (int <caret>a)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "function (int <caret>a)",
           "function (int <caret>a)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @VimBehaviorDiffers(
    originalVimAfter = "function (int <caret>a, int b)",
    description = "Should work the same as testFunctionWithSpaceAfterName"
  )
  public void testFunctionWithSpaceAfterNameWithTwoArgs() {
    doTest(parseKeys("dia"),
           "function (int <caret>a, int b)",
           "function (, int b)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "function (int <caret>a, int b)",
           "function (int b)",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDeleteInIf() {
    doTest(parseKeys("dia"),
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           "class MyClass{ public int myFun() { if (tr<caret>ue) { somFunction(); } } }",
           CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testParseVariablePairs() {
    assertPluginError(false);
    setArgTextObjPairsVariable("[:], (:)");
    doTest(parseKeys("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("expecting ':', but got '(' instead");

    setArgTextObjPairsVariable("[:](:)");
    doTest(parseKeys("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("expecting ',', but got '(' instead");

    setArgTextObjPairsVariable("=:=");
    doTest(parseKeys("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("open and close brackets must be different");

    setArgTextObjPairsVariable("[:],(:");
    doTest(parseKeys("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("list of pairs is incomplete");

    setArgTextObjPairsVariable("");
    doTest(parseKeys("daa"), "f(a<caret>)", "f(a<caret>)", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(true);
    assertPluginErrorMessageContains("list of pairs is incomplete");

    setArgTextObjPairsVariable("[:],(:)");
    doTest(parseKeys("daa"), "f[a<caret>]", "f[<caret>]", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(false);

    setArgTextObjPairsVariable("::;");
    doTest(parseKeys("daa"), "f: a<caret> ;", "f:<caret>;", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    assertPluginError(false);

  }

  public void testCppLambaArguments() {
    setArgTextObjPairsVariable("[:],(:),{:},<:>");
    doTest(parseKeys("daa"),
      "[capture1, c = <caret>capture2] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
      "[capture1] { return Clazz<int, bool>{ctorParam1, ctorParam2}; }",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
      "[capture1, c = capture2] { return Clazz<int,<caret> bool>{ctorParam1, ctorParam2}; }",
      "[capture1, c = capture2] { return Clazz<int>{ctorParam1, ctorParam2}; }",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
    doTest(parseKeys("daa"),
      "[capture1, c = capture2] { return Clazz<int, bool>{ctorPar<caret>am1, ctorParam2}; }",
      "[capture1, c = capture2] { return Clazz<int, bool>{ctorParam2}; }",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

}
