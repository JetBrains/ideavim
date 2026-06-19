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
import kotlin.test.assertTrue

/**
 * Operators combined with motions on folded regions (zj/zk boundary motions, linewise
 * delete/yank/change on collapsed folds, etc.).
 */
class FoldMotionOperatorTest : FoldActionTestBase() {

  // ============== Linewise operators on collapsed folds ==============

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete content of folded region`() {
    configureByJavaText(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText("zc")
    typeText("dd")
    assertState(
      """
          class TestClass {
          ${c}}
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete only header line when fold is open`() {
    configureByJavaText(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()
    assertAllFoldsAreOpen()

    typeText("dd")
    assertState(
      """
          class TestClass {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should yank content of folded region`() {
    configureByJavaText(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("yy")

    val context = injector.executionContextManager.getEditorExecutionContext(fixture.editor.vim)
    val regText = injector.registerGroup.getRegister(fixture.editor.vim, context, '0')!!.text
    assertTrue(regText.contains("public void method()"))
    assertTrue(regText.contains("line 1"))
    assertTrue(regText.contains("line 2"))
    assertTrue(regText.contains("line 3"))

    assertState(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should change content of folded region`() {
    configureByJavaText(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("cc")
    assertState(
      """
          class TestClass {
              ${c}
          }
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete only one character with dl mid-line on collapsed fold header`() {
    configureByJavaText(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("dl")
    assertState(
      """
          class TestClass {
              public${c}void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete folded content with dl at end of collapsed fold header line`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {${c}
                  System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("dl")
    assertState(
      """
          class TestClass {
              public void method()${c} 
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete manual fold content with dd`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("zf2j"))
    typeText("dd")
    assertState(
      """
          class TestClass {
              public void method() {
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete outer folded region with dd on outer header`() {
    configureByJavaText(
      """
          class TestClass {
              public${c} void method() {
                  System.out.println("line 1");
                  if (true) {
                      System.out.println("line 2");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()
    typeText("dd")
    assertState(
      """
          class TestClass {
          ${c}}
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete closed fold and next line with 2dd`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if ${c}(true) {
                      System.out.println("line 1");
                  }
                  System.out.println("line 2");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("2dd")
    assertState(
      """
          class TestClass {
              public void method() {
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should yank collapsed inner if block with yy`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("before");
                  if ${c}(true) {
                      System.out.println("inner");
                  }
                  System.out.println("after");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("yy")

    val context = injector.executionContextManager.getEditorExecutionContext(fixture.editor.vim)
    val regText = injector.registerGroup.getRegister(fixture.editor.vim, context, '0')!!.text
    assertTrue(regText.contains("if (true)"))
    assertTrue(regText.contains("inner"))
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `should delete manual zf fold inside IDE fold with dd`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("keep");
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText(injector.parser.parseKeys("zf2j"))
    typeText("dd")
    assertState(
      """
          class TestClass {
              public void method() {
                  System.out.println("keep");
              ${c}}
          }
      """.trimIndent(),
    )
  }

  // ============== zj/zk boundary motion operators ==============

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
    assertTrue(regText.contains("int x = 5"))
    assertTrue(regText.contains("int y = 10"))
    assertTrue(regText.contains("public void method()"))
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

  // ============== Nested / inner collapsed folds (need implementation) ==============

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `d2j through collapsed inner if inside method preserves method closing brace`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}int x = 5;
                  int y = 10;
                  if (true) {
                      System.out.println("a");
                      System.out.println("b");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("j")
    typeText("j")
    typeText("zc")
    typeText("k")
    typeText("k")
    typeText("d2j")
    assertState(
      """
          class TestClass {
              public void method() {
              ${c}}
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `dd on collapsed inner if header deletes entire if block`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("before");
                  if ${c}(true) {
                      System.out.println("inner");
                  }
                  System.out.println("after");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("dd")
    assertState(
      """
          class TestClass {
              public void method() {
                  System.out.println("before");
                  ${c}System.out.println("after");
              }
          }
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `cc on collapsed inner if header replaces entire if block`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("before");
                  if ${c}(true) {
                      System.out.println("inner");
                  }
                  System.out.println("after");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("cc")
    assertState(
      """
          class TestClass {
              public void method() {
                  System.out.println("before");
                  ${c}
                  System.out.println("after");
              }
          }
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `dd on collapsed empty method deletes entire empty method`() {
    configureByJavaText(
      """
          class TestClass {
              public void empty() {
              ${c}}
              public void method() {
                  System.out.println("line");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("zc")
    typeText("dd")
    assertState(
      """
        class TestClass {
            ${c}public void method() {
                System.out.println("line");
            }
        }
      """.trimIndent(),
    )
  }

  // ============== zk operator variants (need implementation) ==============

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `yzk yanks from cursor to previous fold end`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
              int x = 5;
              ${c}int y = 10;
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("yzk")

    val context = injector.executionContextManager.getEditorExecutionContext(fixture.editor.vim)
    val regText = injector.registerGroup.getRegister(fixture.editor.vim, context, '0')!!.text
    assertTrue(regText.contains("int x = 5"))
    assertTrue(regText.contains("int y = 10"))
    assertTrue(!regText.contains("public void method()"))
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `czk changes from cursor to previous fold end`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              }
              int x = 5;
              ${c}int y = 10;
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    typeText("czk")
    assertState(
      """
          class TestClass {
              public void method() {
                  System.out.println("a");
                  System.out.println("b");
              
          }
      """.trimIndent(),
    )
    assertMode(Mode.INSERT)
  }
}
