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
import com.intellij.openapi.util.TextRange
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import kotlin.test.assertEquals

abstract class FoldActionTestBase : VimJavaTestCase() {

  protected fun updateFoldRegions() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.foldingModel.runBatchFoldingOperation {
        CodeFoldingManager.getInstance(fixture.project).updateFoldRegions(fixture.editor)
      }
    }
  }

  protected fun closeAllFolds() {
    typeText(injector.parser.parseKeys("zM"))
  }

  protected fun openAllFolds() {
    typeText(injector.parser.parseKeys("zR"))
  }

  protected fun toggleFoldWithZa() {
    typeText(injector.parser.parseKeys("za"))
  }

  protected fun toggleFoldRecursivelyWithZA() {
    typeText(injector.parser.parseKeys("zA"))
  }

  protected fun reduceFoldingWithZr() {
    typeText(injector.parser.parseKeys("zr"))
  }

  protected fun moreFoldingWithZm() {
    typeText(injector.parser.parseKeys("zm"))
  }

  protected fun assertMethodFoldIsOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions.sortedBy { it.startOffset }
      val methodFold = allFolds.firstOrNull {
        fixture.editor.document.getLineNumber(it.startOffset) == 1
      }
      assertEquals(true, methodFold?.isExpanded, "Method fold should be open")
    }
  }

  protected fun assertNestedIfBlockIsClosed() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions.sortedBy { it.startOffset }
      val ifFold = allFolds.getOrNull(1)
      assertEquals(false, ifFold?.isExpanded, "Nested if-block fold should be closed")
    }
  }

  protected fun assertAllFoldsAreOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions
      allFolds.forEach { fold ->
        assertEquals(true, fold.isExpanded, "All folds should be expanded")
      }
    }
  }

  protected fun assertAllFoldsAreClosed() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions
      val closedCount = allFolds.count { !it.isExpanded }
      assertEquals(allFolds.size, closedCount, "All folds should be closed")
    }
  }

  protected fun assertOnlyOneFoldIsOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions
      val openCount = allFolds.count { it.isExpanded }
      assertEquals(1, openCount, "za should open only one fold level")
    }
  }

  protected fun assertAllMethodFoldsAreOpen() {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions.sortedBy { it.startOffset }
      val methodFolds = allFolds.filter { fold ->
        val foldText = fixture.editor.document.getText(TextRange(fold.startOffset, fold.endOffset))
        foldText.contains("public void method")
      }
      methodFolds.forEach { fold ->
        assertEquals(true, fold.isExpanded, "All method folds should be open after zr")
      }
    }
  }

  protected fun assertFoldStateAtCursor(expanded: Boolean?) {
    ApplicationManager.getApplication().invokeAndWait {
      val offset = fixture.editor.caretModel.offset
      val line = fixture.editor.document.getLineNumber(offset)
      val fold = FoldingUtil.findFoldRegionStartingAtLine(fixture.editor, line)
      assertEquals(expanded, fold?.isExpanded)
    }
  }

  protected fun assertDeepestFoldsAreClosed(count: Int) {
    ApplicationManager.getApplication().invokeAndWait {
      val allFolds = fixture.editor.foldingModel.allFoldRegions.sortedBy { it.startOffset }
      val deepFolds = allFolds.takeLast(count)
      deepFolds.forEach { fold ->
        assertEquals(false, fold.isExpanded, "Deep folds should still be closed")
      }
    }
  }
}
