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

class IncrementFoldingTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWithZm() {
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

    // First zm should close innermost fold level globally
    moreFoldingWithZm()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()

    // Second zm should close all folds
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingMultipleTimes() {
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

    // Multiple zm commands should progressively close folds
    moreFoldingWithZm()
    moreFoldingWithZm()
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWhenAllClosed() {
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

    // zm on already closed folds should be a no-op
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingVsCloseAll() {
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

    // zm works incrementally (one level at a time) globally
    moreFoldingWithZm()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()

    // zM closes all at once
    openAllFolds()
    closeAllFolds()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWorksGloballyNotJustAtCursor() {
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
    openAllFolds()

    // zm should close one level of ALL folds in the window, not just at cursor
    // Cursor is on method2, but zm should affect method1 as well
    moreFoldingWithZm()

    // Both method folds should still be open, but if blocks should be closed
    assertAllMethodFoldsAreOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingOnEmptyFile() {
    configureByJavaText("")
    updateFoldRegions()

    // zm on empty file should not crash
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingOnSingleLine() {
    configureByJavaText("class A {}")
    updateFoldRegions()

    // zm on single line file with no folds should not crash
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingInVisualMode() {
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

    // Enter visual mode and then use zm
    typeText(injector.parser.parseKeys("v"))
    moreFoldingWithZm()

    // Should close innermost fold level
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWithMultipleFoldsAtSameDepth() {
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
    openAllFolds()

    // First zm should close ALL if blocks (all at same depth level)
    moreFoldingWithZm()
    assertAllMethodFoldsAreOpen()

    // Verify at least one if block is closed (they should all be)
    assertNestedIfBlockIsClosed()

    // Second zm should close all method folds
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWithUnevenNesting() {
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
    openAllFolds()

    // First zm closes while block (depth 2)
    moreFoldingWithZm()
    assertAllMethodFoldsAreOpen()

    // Second zm closes all if blocks (depth 1)
    moreFoldingWithZm()
    assertAllMethodFoldsAreOpen()

    // Third zm closes all method folds (depth 0)
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWithCount() {
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
    openAllFolds()

    // 3zm should close 3 levels at once
    typeText(injector.parser.parseKeys("3zm"))
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testMoreFoldingWithCountPartialClose() {
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
    openAllFolds()

    // 2zm should close 2 deepest levels (for and while, but not if and method)
    typeText(injector.parser.parseKeys("2zm"))

    // Verify method and if are still open
    assertMethodFoldIsOpen()

    // Verify while and for blocks are closed
    assertDeepestFoldsAreClosed(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testZrAndZmAreInverses() {
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

    // zr then zm should return to original state
    reduceFoldingWithZr()
    moreFoldingWithZm()
    assertAllFoldsAreClosed()

    // Multiple zr followed by same number of zm
    reduceFoldingWithZr()
    reduceFoldingWithZr()
    moreFoldingWithZm()
    moreFoldingWithZm()
    assertAllFoldsAreClosed()
  }
}
