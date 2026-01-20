/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.folding.impl.FoldingUtil
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChangeActionJavaTest : VimJavaTestCase() {
  // VIM-511 |.|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testRepeatWithParensAndQuotesAutoInsertion() {
    configureByJavaText(
      """
  class C $c{
  }
  
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("o" + "foo(\"<Right>, \"<Right><Right>;" + "<Esc>" + "."))
    assertState(
      """class C {
    foo("", "");
    foo("", "");
}
""",
    )
  }

  // VIM-511 |.|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun testDeleteBothParensAndStartAgain() {
    configureByJavaText(
      """
  class C $c{
  }
  
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("o" + "C(" + "<BS>" + "(int i) {}" + "<Esc>" + "."))
    assertState(
      """class C {
    C(int i) {}
    C(int i) {}
}
""",
    )
  }

  // VIM-511 |.|
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  @VimBehaviorDiffers(
    originalVimAfter = """
    class C {
      C(int i) {
          i = 3;
      }
      C(int i) {
          i = 3;
      }
    }
  """, description = """The bracket should be on the new line.
    |This behaviour was explicitely broken as we migrate to the new handlers and I can't support it"""
  )
  fun testAutoCompleteCurlyBraceWithEnterWithinFunctionBody() {
    configureByJavaText(
      """
  class C $c{
  }
  
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("o" + "C(" + "<BS>" + "(int i) {" + "<Enter>" + "i = 3;" + "<Esc>" + "<Down>" + "."))
    assertState(
      """class C {
    C(int i) {
        i = 3;
    }
    C(int i) {
    i = 3;}
}
""",
    )
  }

  // VIM-287 |zc| |O|
  @Test
  fun testInsertAfterFold() {
    configureByJavaText(
      """$c/**
 * I should be fold
 * a little more text
 * and final fold
 */
and some text after""",
    )
    typeText(injector.parser.parseKeys("zc" + "G" + "O"))
    assertState(
      """/**
 * I should be fold
 * a little more text
 * and final fold
 */
$c
and some text after""",
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testInsertAfterToggleFold() {
    configureByJavaText(
      """
          $c/**
           * I should be fold
           * a little more text
           * and final fold
           */
          and some text after
      """.trimIndent(),
    )
    updateFoldRegions()
    assertFoldState(0, true)

    typeText(injector.parser.parseKeys("za"))
    assertFoldState(0, false)

    typeText(injector.parser.parseKeys("za"))
    assertFoldState(0, true)
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun testInsertBeforeFold() {
    configureByJavaText(
      """
          $c/**
           * I should be fold
           * a little more text
           * and final fold
           */
          and some text after
      """.trimIndent(),
    )
    updateFoldRegions()
    setFoldState(0, false)

    typeText(injector.parser.parseKeys("o"))
    assertState(
      """
            /**
             * I should be fold
             * a little more text
             * and final fold
             */
            $c
            and some text after
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.FOLDING)
  @Test
  fun toggleJavaClass() {
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

    typeText(injector.parser.parseKeys("za"))
    assertFoldStateAtCursor(false)

    typeText(injector.parser.parseKeys("za"))
    assertFoldStateAtCursor(true)
  }

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

    typeText(injector.parser.parseKeys("zA"))
    assertFoldStateAtCursor(false)

    typeText(injector.parser.parseKeys("zA"))
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

    typeText(injector.parser.parseKeys("zM"))
    assertFoldStateAtCursor(false)

    typeText(injector.parser.parseKeys("za"))
    assertFoldStateAtCursor(true)

    typeText(injector.parser.parseKeys("zA"))
    assertFoldStateAtCursor(false)
  }

  private fun updateFoldRegions() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
      }
    }
  }

  private fun assertFoldState(line: Int, expanded: Boolean) {
    ApplicationManager.getApplication().invokeAndWait {
      val fold = FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, line)
      assertEquals(expanded, fold?.isExpanded)
    }
  }

  private fun assertFoldStateAtCursor(expanded: Boolean) {
    ApplicationManager.getApplication().invokeAndWait {
      val offset = fixture.editor.caretModel.offset
      val line = fixture.editor.document.getLineNumber(offset)
      val fold = fixture.editor.foldingModel.allFoldRegions.firstOrNull {
        val foldLine = fixture.editor.document.getLineNumber(it.startOffset)
        foldLine == line || (it.startOffset <= offset && offset <= it.endOffset)
      }
      assertEquals(expanded, fold?.isExpanded)
    }
  }

  private fun setFoldState(line: Int, expanded: Boolean) {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        val fold = FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, line)
          ?: error("Expected fold at line $line")
        fold.isExpanded = expanded
      }
    }
  }
}