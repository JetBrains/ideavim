/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.fold

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NavigateBetweenFoldsTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj moves to next fold start`() {
    configureByJavaText(
      """
          ${c}class TestClass {
              public void method1() {
                  System.out.println("test1");
                  System.out.println("test1");
              }
              public void method2() {
                  System.out.println("test2");
                  System.out.println("test1");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(1)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj moves from first fold to second fold`() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj at last fold does not move`() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void me${c}thod2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj with count moves to nth next fold`() {
    configureByJavaText(
      """
          ${c}class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method3() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("2zj")

    assertCaretOnLine(5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj moves to nested fold`() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("a");
                      System.out.println("b");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj from inside nested fold moves to next sibling`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("a");
                      System.out.pri${c}ntln("b");
                  }
                  if (false) {
                      System.out.println("a");
                      System.out.println("b");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(6)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj works with closed folds`() {
    configureByJavaText(
      """
          ${c}class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    typeText("zj")

    assertCaretOnLine(1)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj from line before fold moves to fold`() {
    configureByJavaText(
      """
          class TestClass {
              ${c}int x = 5;
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk moves to previous fold end`() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void me${c}thod2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")

    assertCaretOnLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk at first fold does not move`() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")

    assertCaretOnLine(1)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk with count moves to nth previous fold`() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void me${c}thod3() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("2zk")

    assertCaretOnLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk from after all folds moves to last fold`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
              int ${c}x = 5;
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")

    assertCaretOnLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk works with closed folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void me${c}thod2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    typeText("zk")

    assertCaretOnLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk from line after fold moves to fold end`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
              int ${c}x = 5;
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")

    assertCaretOnLine(4)
  }

  // ============ Combined zj and zk tests ============

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj followed by zk returns near original position`() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")
    assertCaretOnLine(5)

    typeText("zk")
    assertCaretOnLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test multiple zj navigates through all folds`() {
    configureByJavaText(
      """
          ${c}class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method3() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")
    assertCaretOnLine(1)

    typeText("zj")
    assertCaretOnLine(5)

    typeText("zj")
    assertCaretOnLine(9)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test multiple zk navigates through all folds backwards`() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void me${c}thod3() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")
    assertCaretOnLine(8)

    typeText("zk")
    assertCaretOnLine(4)

    typeText("zk")
    assertCaretOnLine(4)
  }

  // ============ Edge cases ============

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj in file with no folds`() {
    configureByJavaText(
      """
          int ${c}x = 5;
          int y = 10;
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk in file with no folds`() {
    configureByJavaText(
      """
          int ${c}x = 5;
          int y = 10;
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")

    assertCaretOnLine(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zj moves cursor to first column of fold line`() {
    configureByJavaText(
      """
          class TestClass {
              ${c}int x = 5;
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zj")

    assertCaretOnLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zk moves cursor to fold end line`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
              ${c}int x = 5;
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zk")

    assertCaretOnLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test dzj deletes from cursor to next fold`() {
    configureByJavaText(
      """
          class TestClass {
              ${c}int x = 5;
              int y = 10;
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("dzj")

    // Linewise delete includes the target line (method declaration)
    assertState(
      """
          class TestClass {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test dzk deletes from cursor to previous fold end`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
              ${c}int x = 5;
              int y = 10;
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("dzk")

    // Linewise delete includes the target line (closing brace of method)
    assertState(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              ${c}int y = 10;
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test yzj yanks from cursor to next fold`() {
    configureByJavaText(
      """
          class TestClass {
              ${c}int x = 5;
              int y = 10;
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("yzj")

    val context = injector.executionContextManager.getEditorExecutionContext(fixture.editor.vim)
    val regText = injector.registerGroup.getRegister(fixture.editor.vim, context, '0')!!.text
    // Linewise yank includes all lines from cursor to fold start (inclusive)
    assertEquals(true, regText.contains("int x = 5"))
    assertEquals(true, regText.contains("int y = 10"))
    assertEquals(true, regText.contains("public void method()"))
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test czj changes from cursor to next fold`() {
    configureByJavaText(
      """
          class TestClass {
              ${c}int x = 5;
              int y = 10;
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("czj")

    // Linewise change deletes lines and enters insert mode
    assertState(
      """
        class TestClass {
            ${c}
                System.out.println("a");
                System.out.println("b");
            }
        }
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test d2zj deletes to second next fold`() {
    configureByJavaText(
      """
          ${c}class TestClass {
              public void method1() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method2() {
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method3() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("d2zj")

    // Deletes from class declaration to method2 (inclusive)
    assertState(
      """
                  System.out.println("a");
                  System.out.println("b");
              }
              public void method3() {
                  System.out.println("a");
                  System.out.println("b");
              }
          }
      """.trimIndent(),
    )
  }


  private fun assertCaretOnLine(expectedLine: Int) {
    val actualLine = fixture.editor.caretModel.logicalPosition.line
    assertEquals(
      expectedLine,
      actualLine,
      "Caret should be on line $expectedLine but was on line $actualLine"
    )
  }
}
