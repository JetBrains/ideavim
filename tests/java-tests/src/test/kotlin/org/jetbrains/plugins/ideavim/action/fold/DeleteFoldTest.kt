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

class DeleteFoldTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldAtCursor() {
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

    // Create a manual fold covering lines 2-3
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 2, endLine = 3)

    // Delete it with zd
    typeText(injector.parser.parseKeys("zd"))

    assertNoFoldAtLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldOnlyDeletesOneFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
                  System.out.println("line 4");
                  System.out.println("line 5");
              }
          }
      """.trimIndent(),
    )

    // Create inner fold first (lines 3-4), then outer
    typeText(injector.parser.parseKeys("j"))
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 3, endLine = 4)

    // Create outer fold (lines 2-7)
    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf4j"))
    assertFoldExists(startLine = 2, endLine = 7)

    // Move to inner fold and delete only it with zd
    typeText(injector.parser.parseKeys("zo")) // open outer fold to access inner
    typeText(injector.parser.parseKeys("j"))  // go to line 3
    typeText(injector.parser.parseKeys("zd"))

    assertNoFoldAtLine(3)
    // Outer fold should still exist
    assertFoldExists(startLine = 2, endLine = 7)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldFromInsideFold() {
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

    // Create a manual fold on lines 2-4
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 2, endLine = 4)

    // Open the fold and move inside
    typeText(injector.parser.parseKeys("zo"))
    typeText(injector.parser.parseKeys("j"))  // go to line 3 (inside fold)

    // zd from inside the fold should delete it
    typeText(injector.parser.parseKeys("zd"))

    assertNoFoldAtLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteFoldOnLineWithNoManualFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("test");
              }
          }
          $c
      """.trimIndent(),
    )

    // zd on line with no manual fold should be a no-op (not crash)
    typeText(injector.parser.parseKeys("zd"))

    // Should not crash, state unchanged
    assertNoFoldAtLine(5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldDoesNotAffectIdeFolds() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // Create a manual fold on lines 2-3
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 2, endLine = 3)

    // Delete the manual fold
    typeText(injector.parser.parseKeys("zd"))

    assertNoFoldAtLine(2)
    // IDE-generated method fold should still exist
    assertFoldExists(startLine = 1, endLine = 4)
  }

  // ============== zD tests (delete manual folds recursively) ==============

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldsRecursively() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
                  System.out.println("line 4");
                  System.out.println("line 5");
              }
          }
      """.trimIndent(),
    )

    // First create inner fold (lines 3-4) - must create inner before outer
    typeText(injector.parser.parseKeys("j"))
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 3, endLine = 4)

    // Go back and create outer fold (lines 2-5)
    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 2, endLine = 5)

    // zD should delete both outer and inner folds
    typeText(injector.parser.parseKeys("zD"))

    assertNoFoldAtLine(2)
    assertNoFoldAtLine(3)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldsRecursivelyDeeplyNested() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
                  System.out.println("line 4");
                  System.out.println("line 5");
                  System.out.println("line 6");
              }
          }
      """.trimIndent(),
    )

    // Create folds from innermost to outermost - must create inner before outer
    // Create inner fold (lines 4-5)
    typeText(injector.parser.parseKeys("jj"))
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 4, endLine = 5)

    // Create middle fold (lines 3-6)
    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 3, endLine = 6)

    // Create outer fold (lines 2-7)
    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 2, endLine = 7)

    // zD should delete all three folds
    typeText(injector.parser.parseKeys("zD"))

    assertNoFoldAtLine(2)
    assertNoFoldAtLine(3)
    assertNoFoldAtLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldsRecursivelyFromNestedFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
                  System.out.println("line 4");
                  System.out.println("line 5");
              }
          }
      """.trimIndent(),
    )

    // Create folds from innermost to outermost - must create inner before outer
    // Create inner fold (lines 4-5)
    typeText(injector.parser.parseKeys("jj"))
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 4, endLine = 5)

    // Create middle fold (lines 3-6)
    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 3, endLine = 6)

    // Create outer fold (lines 2-7)
    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 2, endLine = 7)

    // Open outer fold to access middle fold, then zD on middle fold
    // should delete middle and inner, but not outer
    typeText(injector.parser.parseKeys("zo"))
    typeText(injector.parser.parseKeys("j"))
    typeText(injector.parser.parseKeys("zD"))

    assertNoFoldAtLine(3)
    assertNoFoldAtLine(4)
    // Outer fold should still exist
    assertFoldExists(startLine = 2, endLine = 7)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteFoldsRecursivelyOnLineWithNoManualFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("test");
              }
          }
          $c
      """.trimIndent(),
    )

    // zD on line with no manual fold should be a no-op (not crash)
    typeText(injector.parser.parseKeys("zD"))

    // Should not crash
    assertNoFoldAtLine(5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteFoldsRecursivelyOnSingleManualFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
              }
          }
      """.trimIndent(),
    )

    // Create a single manual fold
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 2, endLine = 3)

    // zD on single fold (no nested) should work same as zd
    typeText(injector.parser.parseKeys("zD"))

    assertNoFoldAtLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testDeleteManualFoldsRecursivelyDoesNotAffectIdeFolds() {
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
    updateFoldRegions()

    // Create manual folds - inner first, then outer
    typeText(injector.parser.parseKeys("j"))
    typeText(injector.parser.parseKeys("zfj"))
    assertFoldExists(startLine = 3, endLine = 4)

    typeText(injector.parser.parseKeys("k"))
    typeText(injector.parser.parseKeys("zf2j"))
    assertFoldExists(startLine = 2, endLine = 5)

    // Delete recursively
    typeText(injector.parser.parseKeys("zD"))

    assertNoFoldAtLine(2)
    assertNoFoldAtLine(3)
    // IDE-generated method fold should still exist
    assertFoldExists(startLine = 1, endLine = 5)
  }
}
