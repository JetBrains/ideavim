/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class MultipleCaretsJavaTest : VimJavaTestCase() {
  @Test
  fun testMotionMethodNextEndAction() {
    configureByJavaText(
      """public class Foo {
    private static void firstMethod(int argument) {
        // Do som${c}ething...
    }
    ${c}private static int x$c;    private static void secondMethod(String argument) {
        // Do something.$c..
    }
}""",
    )
    typeText(injector.parser.parseKeys("]M"))
    assertState(
      """public class Foo {
    private static void firstMethod(int argument) {
        // Do something...
    $c}
    private static int x$c;    private static void secondMethod(String argument) {
        // Do something...
    $c}
}""",
    )
  }

  @Test
  fun testMotionMethodNextStartAction() {
    configureByJavaText(
      """public class Foo {
 $c   private static void firstMethod(int argument) {
        // Do som${c}ething...
    }
    ${c}private static int x$c;    private static void secondMethod(String argument) {
        // Do something.$c..
    }
}""",
    )
    typeText(injector.parser.parseKeys("]m"))
    assertState(
      """public class Foo {
    private static void firstMethod(int argument) $c{
        // Do something...
    }
    ${c}private static int x;    private static void secondMethod(String argument) $c{
        // Do something...
    }
}""",
    )
  }

  @Test
  fun testMotionMethodPreviousEndAction() {
    configureByJavaText(
      """public class Foo {
 $c   private static void firstMethod(int argument) {
        // Do som${c}ething...
    }
    ${c}private static int x$c;    private static void secondMethod(String argument) {
        // Do something.$c..
    }
}""",
    )
    typeText(injector.parser.parseKeys("[M"))
    assertState(
      """public class Foo {
    private static void firstMethod(int argument) {
        // Do something...
    $c}
    private static int x$c;    private static void secondMethod(String argument) {
        // Do something...
    }
}""",
    )
  }

  @Test
  fun testMotionMethodPreviousStartAction() {
    configureByJavaText(
      """public class Foo {
 $c   private static void firstMethod(int argument) {
        // Do som${c}ething...
    }
    ${c}private static int x$c;    private static void secondMethod(String argument) {
        // Do something.$c..
    }
}""",
    )
    typeText(injector.parser.parseKeys("[m"))
    assertState(
      """public class Foo $c{
    private static void firstMethod(int argument) $c{
        // Do something...
    }
    ${c}private static int x;    private static void secondMethod(String argument) $c{
        // Do something...
    }
}""",
    )
  }

  // com.maddyhome.idea.vim.action.change.change
  @Test
  fun testAutoIndentLinesVisualAction() {
    configureByJavaText(
      """${c}public class Foo {
private boolean x;
                         private boolean y;
private boolean z;
${c}public void foo() {
x = true; // This will be indented
}
public void bar() {
y = true; // And this will not
}
}
""",
    )
    typeText(injector.parser.parseKeys("V2j="))
    assertState(
      """${c}public class Foo {
    private boolean x;
    private boolean y;
private boolean z;
    ${c}public void foo() {
        x = true; // This will be indented
    }
public void bar() {
y = true; // And this will not
}
}
""",
    )
  }

  @Test
  fun testAutoIndentRange() {
    val before = "cl${c}ass C {\n C(int i) {\nmy${c}I = i;\n}\n private int myI;\n}"
    configureByJavaText(before)
    typeText(injector.parser.parseKeys("v2j="))
    val after = """${c}class C {
    C(int i) {
        myI = i;
    }
    private int myI;
}"""
    assertState(after)
  }

  @Test
  fun testAutoIndentMotion() {
    val before = "cl${c}ass C {\n C(int i) {\nmy${c}I = i;\n}\n private int myI;\n}"
    configureByJavaText(before)
    typeText(injector.parser.parseKeys("=3j"))
    val after = """${c}class C {
    C(int i) {
        ${c}myI = i;
    }
    private int myI;
}"""
    assertState(after)
  }

  @Test
  fun testAutoIndentLines() {
    val before = "class C {\n C$c(int i) {\nmyI = i;\n}\n p${c}rivate int myI;\n}"
    configureByJavaText(before)
    typeText(injector.parser.parseKeys("=="))
    val after = "class C {\n    ${c}C(int i) {\nmyI = i;\n}\n    ${c}private int myI;\n}"
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorWithIndention() {
    val before = """class C {
    C(int i) {
        myI = i;
    }
    ${c}private int myI = 0;
    {
        ${c}private int myJ = 0;
    }
    ${c}private int myK = 0;
}"""
    configureByJavaText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "private int myK = 0;\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*P"))
    val after = """class C {
    C(int i) {
        myI = i;
    }
    ${c}private int myK = 0;
    private int myI = 0;
    {
        ${c}private int myK = 0;
        private int myJ = 0;
    }
    ${c}private int myK = 0;
    private int myK = 0;
}"""
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursorWithIndention() {
    val before = """class C {
    C(int i) {
        myI = i;
    }
    ${c}private int myI = 0;
    {
        ${c}private int myJ = 0;
    }
    ${c}private int myK = 0;
}"""
    configureByJavaText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "private int myK = 0;", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*p"))
    val after = """class C {
    C(int i) {
        myI = i;
    }
    private int myI = 0;
    ${c}private int myK = 0;
    {
        private int myJ = 0;
        ${c}private int myK = 0;
    }
    private int myK = 0;
    ${c}private int myK = 0;
}"""
    assertState(after)
  }
}