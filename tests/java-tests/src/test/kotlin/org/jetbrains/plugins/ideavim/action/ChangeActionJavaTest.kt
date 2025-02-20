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

  // VIM-566
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
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
        assertEquals(FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, 0)!!.isExpanded, true)
      }
    }
    typeText(injector.parser.parseKeys("za"))
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        assertEquals(FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, 0)!!.isExpanded, false)
      }
    }
    typeText(injector.parser.parseKeys("za"))
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        assertEquals(FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, 0)!!.isExpanded, true)
      }
    }
  }

  // VIM-287 |zc| |o|
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

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
        FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, 0)!!.isExpanded = false
      }
    }

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
}