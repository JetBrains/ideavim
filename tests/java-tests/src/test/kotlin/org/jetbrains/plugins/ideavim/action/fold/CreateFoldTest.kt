/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.fold

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.junit.jupiter.api.Test

class CreateFoldTest : FoldActionTestBase() {

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithDownMotion() {
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

    typeText(injector.parser.parseKeys("zfj"))

    assertFoldExists(startLine = 2, endLine = 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithVisualSelection() {
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

    typeText(injector.parser.parseKeys("Vjzf"))

    assertFoldExists(startLine = 2, endLine = 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithCount() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("line 1");
                  System.out.println("line 2");
                  System.out.println("line 3");
                  System.out.println("line 4");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText(injector.parser.parseKeys("zf2j"))

    assertFoldExists(startLine = 2, endLine = 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithBraceTextObject() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      ${c}System.out.println("test");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText(injector.parser.parseKeys("zfa{"))

    assertFoldExists(startLine = 2, endLine = 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithUpMotion() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("line 1");
                  ${c}System.out.println("line 2");
                  System.out.println("line 3");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText(injector.parser.parseKeys("zfk"))

    assertFoldExists(startLine = 2, endLine = 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreatedFoldIsClosed() {
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

    typeText(injector.parser.parseKeys("zfj"))

    assertFoldExistsAndClosed(startLine = 2, endLine = 3)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldToEndOfFile() {
    configureByJavaText(
      """
          class TestClass {
              public void ${c}method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText(injector.parser.parseKeys("zfG"))

    assertFoldExists(startLine = 1, endLine = 5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldToBeginningOfFile() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  System.out.println("line 1");
                  System.out.println("line 2");
              }
          ${c}}
      """.trimIndent(),
    )
    updateFoldRegions()

    typeText(injector.parser.parseKeys("zfgg"))

    assertFoldExists(startLine = 0, endLine = 5)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldOnLastLineWithDownMotion() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
              }
          ${c}}
      """.trimIndent(),
    )
    updateFoldRegions()

    // zfj on last line should not create a fold (no line below)
    typeText(injector.parser.parseKeys("zfj"))

    assertNoFoldAtLine(4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldOnFirstLineWithUpMotion() {
    configureByJavaText(
      """
          ${c}class TestClass {
              public void method() {
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // zfk on first line should not create a fold (no line above)
    typeText(injector.parser.parseKeys("zfk"))

    assertNoFoldAtLine(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldOnSingleLineFile() {
    configureByJavaText("${c}class A {}")
    updateFoldRegions()

    // zfj on single line file should not crash
    typeText(injector.parser.parseKeys("zfj"))

    assertNoFoldAtLine(0)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithWordMotion() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  ${c}System.out.println("test");
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // zfe folds from cursor to end of word "System"
    typeText(injector.parser.parseKeys("zfe"))

    assertFoldCreatedOnLine(2)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateNestedFold() {
    configureByJavaText(
      """
          class TestClass {
              public void method() {
                  if (true) {
                      ${c}System.out.println("line 1");
                      System.out.println("line 2");
                  }
              }
          }
      """.trimIndent(),
    )
    updateFoldRegions()

    // Create a fold inside the existing method/if folds
    typeText(injector.parser.parseKeys("zfj"))

    assertFoldExists(startLine = 3, endLine = 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithVisualBlockSelection() {
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

    // Visual block selection then zf
    typeText(injector.parser.parseKeys("<C-v>jjzf"))

    assertFoldExists(startLine = 2, endLine = 4)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testCreateFoldWithCharacterVisualSelection() {
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

    // Character-wise visual selection spanning two lines
    typeText(injector.parser.parseKeys("vj\$zf"))

    assertFoldExists(startLine = 2, endLine = 3)
  }

  private fun assertFoldCreatedOnLine(line: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val fold = findFoldAtLine(line)
      kotlin.test.assertNotNull(fold, "Expected fold to be created on line $line")
    }
  }
}
