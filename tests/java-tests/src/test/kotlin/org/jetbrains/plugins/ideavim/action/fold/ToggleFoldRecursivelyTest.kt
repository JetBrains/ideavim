/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.fold

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.api.Test

class ToggleFoldRecursivelyTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursively() {
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

    toggleFoldRecursivelyWithZA()
    assertFoldStateAtCursor(false)

    toggleFoldRecursivelyWithZA()
    assertFoldStateAtCursor(true)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursivelyVsNonRecursive() {
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
    assertFoldStateAtCursor(false)

    toggleFoldWithZa()
    assertFoldStateAtCursor(true)

    toggleFoldRecursivelyWithZA()
    assertFoldStateAtCursor(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursivelyOnLineWithNoFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
          $c
      """.trimIndent(),
    )
    updateFoldRegions()

    toggleFoldRecursivelyWithZA()

    // Verify zA on line with no fold is a no-op
    assertState(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.println("test");
                  }
              }
          }
          $c
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursivelyOnNestedFolds() {
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

    // za (non-recursive) opens only immediate fold, nested fold remains closed
    toggleFoldWithZa()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()

    // zA (recursive) opens all nested folds
    closeAllFolds()
    toggleFoldRecursivelyWithZA()
    assertAllFoldsAreOpen()

    // zA (recursive) closes all nested folds
    toggleFoldRecursivelyWithZA()
    assertAllFoldsAreClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursivelyOnMethodLine() {
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

    // zA on method line closes all nested folds recursively
    toggleFoldRecursivelyWithZA()
    assertAllFoldsAreClosed()

    toggleFoldRecursivelyWithZA()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursivelyFromInsideNestedFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      System.out.prin${c}tln("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()

    // zA from inside if-block closes only if-block, method fold stays open
    toggleFoldRecursivelyWithZA()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()

    toggleFoldRecursivelyWithZA()
    assertAllFoldsAreOpen()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testToggleFoldRecursivelyOnNestedFoldLine() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (tr${c}ue) {
                      System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()
    openAllFolds()

    // zA on if line closes if-block, method fold stays open
    toggleFoldRecursivelyWithZA()
    assertMethodFoldIsOpen()
    assertNestedIfBlockIsClosed()
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCompareZAvsZaOnNestedStructure() {
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

    // za opens only one fold level (non-recursive)
    toggleFoldWithZa()
    assertOnlyOneFoldIsOpen()

    // zA opens all nested folds recursively
    closeAllFolds()
    toggleFoldRecursivelyWithZA()
    assertAllFoldsAreOpen()
  }
}
