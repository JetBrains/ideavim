/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.fold

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.api.Test

class SetFoldLevelTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel to 0 closes all folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()

    enterCommand("set foldlevel=0")
    assertCommandOutput("set foldlevel?", "  foldlevel=0")
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel to 1 opens only top-level folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    enterCommand("set foldlevel=1")
    assertCommandOutput("set foldlevel?", "  foldlevel=1")
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel to 2 opens nested folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    enterCommand("set foldlevel=2")
    assertCommandOutput("set foldlevel?", "  foldlevel=2")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel to high number opens all folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    enterCommand("set foldlevel=999")
    // Should be coerced to maxDepth+1 (which is 2 for this file)
    assertCommandOutput("set foldlevel?", "  foldlevel=2")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel to maxDepth plus one opens all folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  if (true) {
                      for (int i = 0; i < 10; i++) {
                          System.out.println("nested");
                      }
                  }
              }
              public void method2() {
                  System.out.println("simple");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // maxDepth for this file is 2 (method -> if -> for), so maxDepth+1 is 3
    enterCommand("set foldlevel=3")
    assertCommandOutput("set foldlevel?", "  foldlevel=3")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel query shows current value`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    enterCommand("set foldlevel=2")
    assertCommandOutput("set foldlevel?", "  foldlevel=2")
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel query on default value`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    assertCommandOutput("set foldlevel?", "  foldlevel=2")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel plus equals increments by one`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    enterCommand("set foldlevel=0")
    enterCommand("set foldlevel+=1")
    assertCommandOutput("set foldlevel?", "  foldlevel=1")
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel minus equals decrements by one`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()

    enterCommand("set foldlevel=2")
    enterCommand("set foldlevel-=1")
    assertCommandOutput("set foldlevel?", "  foldlevel=1")
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel plus equals with count`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    enterCommand("set foldlevel=0")
    enterCommand("set foldlevel+=2")
    assertCommandOutput("set foldlevel?", "  foldlevel=2")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel minus equals with count`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()

    enterCommand("set foldlevel=4")
    enterCommand("set foldlevel-=1")
    assertCommandOutput("set foldlevel?", "  foldlevel=1")
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel plus equals cannot exceed max depth`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // maxDepth is 1, so setting to maxDepth+1=2 then adding 100 should coerce to 2
    enterCommand("set foldlevel=1")
    enterCommand("set foldlevel+=100")
    assertCommandOutput("set foldlevel?", "  foldlevel=2")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel minus equals cannot go below zero`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    enterCommand("set foldlevel=0")
    enterCommand("set foldlevel-=5")
    assertCommandOutput("set foldlevel?", "  foldlevel=0")
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test set foldlevel ampersand resets to default`() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    enterCommand("set foldlevel=0")
    assertAllFoldsAreClosed()

    enterCommand("set foldlevel&")
    // Default is maxDepth, which is 2 for this file
    assertCommandOutput("set foldlevel?", "  foldlevel=2")
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun `test zM with foldlevel already at 0 closes manually opened folds`() {
    configureByJavaText(
      """
          class TestClass {
              public void met${c}hod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // Set foldlevel to 0 (all folds closed)
    enterCommand("set foldlevel=0")
    assertCommandOutput("set foldlevel?", "  foldlevel=0")
    assertAllFoldsAreClosed()

    // Manually open some folds with zo
    typeText(injector.parser.parseKeys("zo"))
    assertMethodFoldIsOpen()

    // Now call zM - foldlevel is already 0, but folds should still close
    closeAllFolds()
    assertCommandOutput("set foldlevel?", "  foldlevel=0")
    assertAllFoldsAreClosed()
  }
}
