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

class DecrementFoldingTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingOnNewWindow() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // On a new window, all folds should be open by default (foldlevel = max depth)
    // zr should be a no-op because foldlevel is already at max
    assertAllFoldsAreOpen()

    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingCannotExceedMaxDepth() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // All folds already open
    assertAllFoldsAreOpen()

    // Multiple zr commands should not increase foldlevel beyond max depth
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()

    // zm should still work to close folds
    moreFoldingWithZm()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWithZr() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // First zr should open outermost fold level globally (entire window)
    reduceFoldingWithZr()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()

    // Second zr should open next fold level globally
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingMultipleTimes() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // Multiple zr commands should progressively open folds
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWhenAllOpen() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()

    // zr on already open folds should be a no-op
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingVsOpenAll() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // zr works incrementally (one level at a time) globally
    reduceFoldingWithZr()
    assertOnlyOneFoldIsOpen()

    // zR opens all at once
    closeAllFolds()
    openAllFolds()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWorksGloballyNotJustAtCursor() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  if (true) {
                      System.out.println("test1");
                  }
              }
              public void met${c}hod2() {
                  if (true) {
                      System.out.println("test2");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // zr should open one level of ALL folds in the window, not just at cursor
    // Cursor is on method2, but zr should affect method1 as well
    reduceFoldingWithZr()

    // Both method1 and method2 should be opened (outermost level)
    assertAllMethodFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingOnEmptyFile() {
    configureByJavaText("")
    updateFoldRegions()

    // zr on empty file should not crash
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingOnSingleLine() {
    configureByJavaText("class A {}")
    updateFoldRegions()

    // zr on single line file with no folds should not crash
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingInVisualMode() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // Enter visual mode and then use zr
    typeText(injector.parser.parseKeys("v"))
    reduceFoldingWithZr()

    // Should open outermost fold level
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWithMultipleFoldsAtSameDepth() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  if (true) {
                      System.out.println("test1");
                  }
              }
              public void method2() {
                  if (true) {
                      System.out.println("test2");
                  }
              }
              public void met${c}hod3() {
                  if (true) {
                      System.out.println("test3");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // First zr should open ALL method folds (all at same depth level)
    reduceFoldingWithZr()
    assertAllMethodFoldsAreOpen()

    // Second zr should open all nested if blocks
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWithUnevenNesting() {
    configureByJavaText(
      """
          class TestClass {
              public void method1() {
                  if (true) {
                      while (true) {
                          System.out.println("deeply nested");
                      }
                  }
              }
              public void met${c}hod2() {
                  if (true) {
                      System.out.println("less nested");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    // First zr opens all method folds (depth 0)
    reduceFoldingWithZr()
    assertAllMethodFoldsAreOpen()

    // Second zr opens all if blocks (depth 1)
    // At this point, 2 method folds and 2 if blocks are open, while block still closed
    reduceFoldingWithZr()

    // Third zr opens while block (depth 2)
    reduceFoldingWithZr()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWithCount() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      while (true) {
                          System.out.println("deeply nested");
                      }
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    typeText(injector.parser.parseKeys("3zr"))
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testReduceFoldingWithCountPartialOpen() {
    configureByJavaText(
      """
          class TestClass {
              public void me${c}thod() {
                  if (true) {
                      while (true) {
                          for (int i = 0; i < 10; i++) {
                              System.out.println("very deeply nested");
                          }
                      }
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    closeAllFolds()

    typeText(injector.parser.parseKeys("2zr"))

    assertMethodFoldIsOpen()
    assertDeepestFoldsAreClosed(2)
  }
}
