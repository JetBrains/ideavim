/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.fold

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.folding.impl.FoldingUtil
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FoldActionJavaTest : VimJavaTestCase() {

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

    // zA from inside if-block closes only if-block, method fold stays open
    moveToPrintlnLine()
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

  private fun updateFoldRegions() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
      }
    }
  }

  private fun closeAllFolds() {
    typeText(injector.parser.parseKeys("zM"))
  }

  private fun openAllFolds() {
    typeText(injector.parser.parseKeys("zR"))
  }

  private fun toggleFoldWithZa() {
    typeText(injector.parser.parseKeys("za"))
  }

  private fun toggleFoldRecursivelyWithZA() {
    typeText(injector.parser.parseKeys("zA"))
  }

  private fun moveToPrintlnLine() {
    typeText(injector.parser.parseKeys("2j"))
  }

  private fun moveToIfLine() {
    typeText(injector.parser.parseKeys("k"))
  }

  private fun assertMethodFoldIsOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions.sortedBy { it.startOffset }
      val methodFold = allFolds.firstOrNull()
      assertEquals(true, methodFold?.isExpanded, "Method fold should be open")
    }
  }

  private fun assertNestedIfBlockIsClosed() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions.sortedBy { it.startOffset }
      val ifFold = allFolds.getOrNull(1)
      assertEquals(false, ifFold?.isExpanded, "Nested if-block fold should be closed")
    }
  }

  private fun assertAllFoldsAreOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions
      allFolds.forEach { fold ->
        assertEquals(true, fold.isExpanded, "All folds should be expanded")
      }
    }
  }

  private fun assertAllFoldsAreClosed() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions
      val closedCount = allFolds.count { !it.isExpanded }
      assertEquals(allFolds.size, closedCount, "All folds should be closed")
    }
  }

  private fun assertOnlyOneFoldIsOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions
      val openCount = allFolds.count { it.isExpanded }
      assertEquals(1, openCount, "za should open only one fold level")
    }
  }


  private fun assertFoldStateAtCursor(expanded: Boolean?) {
    ApplicationManager.getApplication().invokeAndWait {
      val offset = fixture.editor.caretModel.offset
      val line = fixture.editor.document.getLineNumber(offset)
      val fold = FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, line)
      assertEquals(expanded, fold?.isExpanded)
    }
  }
}
